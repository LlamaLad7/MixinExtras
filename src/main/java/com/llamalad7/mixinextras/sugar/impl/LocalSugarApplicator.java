package com.llamalad7.mixinextras.sugar.impl;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.utils.ASMUtils;
import com.llamalad7.mixinextras.utils.CompatibilityHelper;
import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.VarInsnNode;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.injection.modify.LocalVariableDiscriminator;
import org.spongepowered.asm.mixin.injection.modify.LocalVariableDiscriminator.Context;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes.InjectionNode;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.PrettyPrinter;
import org.spongepowered.asm.util.SignaturePrinter;

import java.util.List;

class LocalSugarApplicator extends SugarApplicator {
    public LocalSugarApplicator() {
        super(Local.class);
    }

    @Override
    void preInject(IMixinInfo mixin, List<Pair<Type, AnnotationNode>> sugarInfos, Target target, InjectionNode node) {
        for (Pair<Type, AnnotationNode> sugar : sugarInfos) {
            Type paramType = sugar.getLeft();
            AnnotationNode annotation = sugar.getRight();
            if (annotation.desc.equals(this.annotationDesc)) {
                boolean isArgsOnly = Annotations.<Boolean>getValue(annotation, "argsOnly", (Boolean) false);
                String decorationKey = getTargetNodeKey(paramType, isArgsOnly);
                if (node.hasDecoration(decorationKey)) {
                    continue;
                }
                Context context = CompatibilityHelper.makeLvtContext(mixin, paramType, isArgsOnly, target, node.getCurrentTarget());
                node.decorate(decorationKey, context);
            }
        }
    }

    @Override
    void inject(IMixinInfo mixin, Type paramType, AnnotationNode sugar, Target target, InjectionNode node) {
        LocalVariableDiscriminator discriminator = LocalVariableDiscriminator.parse(sugar);
        Context context = node.getDecoration(getTargetNodeKey(paramType, discriminator.isArgsOnly()));
        int index = discriminator.findLocal(context);
        if (index < 0) {
            throw new IllegalStateException(String.format(
                    "Failed to match a local for %s %s in mixin %s in target %s at instruction %s!",
                    ASMUtils.annotationToString(sugar), ASMUtils.typeToString(paramType), mixin, target, node
            ));
        }
        if (discriminator.printLVT()) {
            printLocals(target, node.getCurrentTarget(), context, paramType, discriminator);
        }
        InsnList insns = new InsnList();
        insns.add(new VarInsnNode(paramType.getOpcode(Opcodes.ILOAD), index));
        target.insertBefore(node, insns);
    }

    private String getTargetNodeKey(Type paramType, boolean isArgsOnly) {
        return String.format("mixinextras_localSugarContext(%s,%s)", paramType, isArgsOnly ? "argsOnly" : "fullFrame");
    }

    private void printLocals(Target target, AbstractInsnNode node, Context context, Type paramType, LocalVariableDiscriminator discriminator) {
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
                .kv("Args only", discriminator.isArgsOnly()).hr()
                .add(context)
                .print(System.err);
    }

    private boolean isImplicit(LocalVariableDiscriminator discriminator, int baseArgIndex) {
        return discriminator.getOrdinal() < 0 && discriminator.getIndex() < baseArgIndex && discriminator.getNames().isEmpty();
    }
}
