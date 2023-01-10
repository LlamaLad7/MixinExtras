package com.llamalad7.mixinextras.sugar.impl;

import com.llamalad7.mixinextras.utils.CompatibilityHelper;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.VarInsnNode;
import org.spongepowered.asm.mixin.injection.modify.InvalidImplicitDiscriminatorException;
import org.spongepowered.asm.mixin.injection.modify.LocalVariableDiscriminator;
import org.spongepowered.asm.mixin.injection.modify.LocalVariableDiscriminator.Context;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes.InjectionNode;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.PrettyPrinter;
import org.spongepowered.asm.util.SignaturePrinter;

class LocalSugarApplicator extends SugarApplicator {
    private final boolean isArgsOnly;

    public LocalSugarApplicator(InjectionInfo info, Type paramType, AnnotationNode sugar) {
        super(info, paramType, sugar);
        this.isArgsOnly = Annotations.getValue(sugar, "argsOnly", (Boolean) false);
    }

    @Override
    void validate(Target target, InjectionNode node) {
        LocalVariableDiscriminator discriminator = LocalVariableDiscriminator.parse(sugar);
        Context context = getOrCreateLocalContext(target, node);
        if (discriminator.printLVT()) {
            printLocals(target, node.getCurrentTarget(), context, discriminator);
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
    void preInject(Target target, InjectionNode node) {
        getOrCreateLocalContext(target, node);
    }

    @Override
    void inject(Target target, InjectionNode node) {
        LocalVariableDiscriminator discriminator = LocalVariableDiscriminator.parse(sugar);
        Context context = node.getDecoration(getLocalContextKey());
        int index = discriminator.findLocal(context);
        if (index < 0) {
            throw new SugarApplicationException("Failed to match a local, this should have been caught during validation.");
        }
        InsnList insns = new InsnList();
        insns.add(new VarInsnNode(paramType.getOpcode(Opcodes.ILOAD), index));
        target.insertBefore(node, insns);
    }

    private Context getOrCreateLocalContext(Target target, InjectionNode node) {
        String decorationKey = getLocalContextKey();
        if (node.hasDecoration(decorationKey)) {
            return node.getDecoration(decorationKey);
        }
        Context context = CompatibilityHelper.makeLvtContext(info, paramType, isArgsOnly, target, node.getCurrentTarget());
        node.decorate(decorationKey, context);
        return context;
    }

    private String getLocalContextKey() {
        return String.format("mixinextras_localSugarContext(%s,%s)", paramType, isArgsOnly ? "argsOnly" : "fullFrame");
    }

    private void printLocals(Target target, AbstractInsnNode node, Context context, LocalVariableDiscriminator discriminator) {
        int baseArgIndex = target.isStatic ? 0 : 1;

        new PrettyPrinter()
                .kvWidth(20)
                .kv("Target Class", target.classNode.name.replace('/', '.'))
                .kv("Target Method", target.method.name)
                .kv("Capture Type", SignaturePrinter.getTypeName(paramType, false))
                .kv("Instruction", "[%d] %s %s", target.insns.indexOf(node), node.getClass().getSimpleName(),
                        Bytecode.getOpcodeName(node.getOpcode())).hr()
                .kv("Match mode", isImplicit(discriminator, baseArgIndex) ? "IMPLICIT (match single)" : "EXPLICIT (match by criteria)")
                .kv("Match ordinal", discriminator.getOrdinal() < 0 ? "any" : discriminator.getOrdinal())
                .kv("Match index", discriminator.getIndex() < baseArgIndex ? "any" : discriminator.getIndex())
                .kv("Match name(s)", discriminator.hasNames() ? discriminator.getNames() : "any")
                .kv("Args only", isArgsOnly).hr()
                .add(context)
                .print(System.err);
    }

    private boolean isImplicit(LocalVariableDiscriminator discriminator, int baseArgIndex) {
        return discriminator.getOrdinal() < 0 && discriminator.getIndex() < baseArgIndex && discriminator.getNames().isEmpty();
    }
}
