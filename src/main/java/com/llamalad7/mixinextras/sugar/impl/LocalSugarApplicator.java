package com.llamalad7.mixinextras.sugar.impl;

import com.llamalad7.mixinextras.injector.StackExtension;
import com.llamalad7.mixinextras.sugar.impl.ref.LocalRefClassGenerator;
import com.llamalad7.mixinextras.sugar.impl.ref.LocalRefUtils;
import com.llamalad7.mixinextras.utils.Decorations;
import com.llamalad7.mixinextras.utils.InjectorUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.VarInsnNode;
import org.spongepowered.asm.mixin.injection.modify.InvalidImplicitDiscriminatorException;
import org.spongepowered.asm.mixin.injection.modify.LocalVariableDiscriminator;
import org.spongepowered.asm.mixin.injection.modify.LocalVariableDiscriminator.Context;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes.InjectionNode;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.util.Annotations;

import java.util.HashMap;
import java.util.Map;

class LocalSugarApplicator extends SugarApplicator {
    private final boolean isArgsOnly;
    private final Type targetLocalType = LocalRefUtils.getTargetType(this.paramType, this.paramGeneric);
    private final boolean isMutable = targetLocalType != paramType;

    LocalSugarApplicator(InjectionInfo info, SugarParameter parameter) {
        super(info, parameter);
        this.isArgsOnly = Annotations.getValue(sugar, "argsOnly", (Boolean) false);
    }

    @Override
    void validate(Target target, InjectionNode node) {
        LocalVariableDiscriminator discriminator = LocalVariableDiscriminator.parse(sugar);
        Context context = InjectorUtils.getOrCreateLocalContext(target, node, info, targetLocalType, isArgsOnly);
        if (discriminator.printLVT()) {
            InjectorUtils.printLocals(target, node.getCurrentTarget(), context, discriminator, targetLocalType, isArgsOnly);
            info.addCallbackInvocation(info.getMethod());
            throw new SugarApplicationException("Application aborted because locals are being printed instead.");
        }
        try {
            if (discriminator.findLocal(context) < 0) {
                throw new SugarApplicationException("Unable to find matching local!");
            }
        } catch (InvalidImplicitDiscriminatorException e) {
            throw new SugarApplicationException("Invalid implicit variable discriminator: ", e);
        }
    }

    @Override
    void prepare(Target target, InjectionNode node) {
        InjectorUtils.getOrCreateLocalContext(target, node, info, targetLocalType, isArgsOnly);
    }

    @Override
    void inject(Target target, InjectionNode node, StackExtension stack) {
        LocalVariableDiscriminator discriminator = LocalVariableDiscriminator.parse(sugar);
        Context context = InjectorUtils.getOrCreateLocalContext(target, node, info, targetLocalType, isArgsOnly);
        int index = discriminator.findLocal(context);
        if (index < 0) {
            throw new SugarApplicationException("Failed to match a local, this should have been caught during validation.");
        }
        if (isMutable) {
            initAndLoadLocalRef(target, node, index, stack);
        } else {
            stack.extra(targetLocalType.getSize());
            target.insns.insertBefore(node.getCurrentTarget(), new VarInsnNode(targetLocalType.getOpcode(Opcodes.ILOAD), index));
        }
    }

    private void initAndLoadLocalRef(Target target, InjectionNode node, int index, StackExtension stack) {
        String refName = LocalRefClassGenerator.getForType(targetLocalType);
        int refIndex = getOrCreateRef(target, node, index, refName, stack);
        stack.extra(1);
        target.insns.insertBefore(node.getCurrentTarget(), new VarInsnNode(Opcodes.ALOAD, refIndex));
    }

    private int getOrCreateRef(Target target, InjectionNode node, int index, String refImpl, StackExtension stack) {
        Map<Integer, Integer> refIndices = node.getDecoration(Decorations.LOCAL_REF_MAP);
        if (refIndices == null) {
            refIndices = new HashMap<>();
            node.decorate(Decorations.LOCAL_REF_MAP, refIndices);
        }
        if (refIndices.containsKey(index)) {
            // Another handler has already created an applicable reference, so we should share it.
            return refIndices.get(index);
        }
        // We have to create the reference ourselves as we are the first to need it.
        int refIndex = target.allocateLocal();
        target.addLocalVariable(refIndex, "ref" + refIndex, 'L' + refImpl + ';');
        // Make and store the reference object, currently uninitialized.
        InsnList construction = new InsnList();
        LocalRefUtils.generateNew(construction, targetLocalType);
        construction.add(new VarInsnNode(Opcodes.ASTORE, refIndex));
        target.insertBefore(node, construction);

        SugarPostProcessingExtension.enqueuePostProcessing(this, () -> {
            // When all injectors have finished applying, we need to initialize the refs before the handler call,
            // and write them back to the target method after. It's important to do this late so they're as tight
            // as possible around the call and we don't have any issues with stale values being used.
            InsnList initialization = new InsnList();
            initialization.add(new VarInsnNode(Opcodes.ALOAD, refIndex));
            initialization.add(new VarInsnNode(targetLocalType.getOpcode(Opcodes.ILOAD), index));
            LocalRefUtils.generateInitialization(initialization, targetLocalType);
            target.insertBefore(node, initialization);
            stack.extra(targetLocalType.getSize() + 1);

            InsnList after = new InsnList();
            after.add(new VarInsnNode(Opcodes.ALOAD, refIndex));
            LocalRefUtils.generateDisposal(after, targetLocalType);
            after.add(new VarInsnNode(targetLocalType.getOpcode(Opcodes.ISTORE), index));
            target.insns.insert(node.getCurrentTarget(), after);
        });
        // Tell future injectors where to find the reference.
        refIndices.put(index, refIndex);
        return refIndex;
    }
}
