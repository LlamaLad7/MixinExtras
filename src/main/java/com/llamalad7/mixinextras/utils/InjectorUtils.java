package com.llamalad7.mixinextras.utils;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.spongepowered.asm.mixin.injection.modify.LocalVariableDiscriminator;
import org.spongepowered.asm.mixin.injection.modify.LocalVariableDiscriminator.Context;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes.InjectionNode;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.PrettyPrinter;
import org.spongepowered.asm.util.SignaturePrinter;

import java.util.List;
import java.util.ListIterator;
import java.util.Map;

public class InjectorUtils {
    private static final MixinExtrasLogger LOGGER = MixinExtrasLogger.get("InjectorUtils");

    public static boolean isVirtualRedirect(InjectionNode node) {
        return node.isReplaced() && node.hasDecoration("redirector") && node.getCurrentTarget().getOpcode() != Opcodes.INVOKESTATIC;
    }

    public static boolean isDynamicInstanceofRedirect(InjectionNode node) {
        AbstractInsnNode originalTarget = node.getOriginalTarget();
        AbstractInsnNode currentTarget = node.getCurrentTarget();

        return originalTarget.getOpcode() == Opcodes.INSTANCEOF
                && currentTarget instanceof MethodInsnNode
                && Type.getReturnType(((MethodInsnNode) currentTarget).desc).equals(Type.getType(Class.class));
    }

    public static void checkForDupedNews(Map<Target, List<InjectionNode>> targets) {
        for (Map.Entry<Target, List<InjectionNode>> entry : targets.entrySet()) {
            for (InjectionNode node : entry.getValue()) {
                AbstractInsnNode currentTarget = node.getCurrentTarget();
                if (currentTarget.getOpcode() == Opcodes.NEW) {
                    if (currentTarget.getNext().getOpcode() == Opcodes.DUP) {
                        node.decorate(Decorations.NEW_IS_DUPED, true);
                    }
                }
            }
        }
    }

    public static boolean isDupedNew(InjectionNode node) {
        AbstractInsnNode currentTarget = node.getCurrentTarget();
        return currentTarget != null && currentTarget.getOpcode() == Opcodes.NEW && node.hasDecoration(Decorations.NEW_IS_DUPED);
    }

    public static boolean isDupedFactoryRedirect(InjectionNode node) {
        AbstractInsnNode originalTarget = node.getOriginalTarget();
        return node.isReplaced() && originalTarget.getOpcode() == Opcodes.NEW && !node.hasDecoration(Decorations.WRAPPED) && node.hasDecoration(Decorations.NEW_IS_DUPED);
    }

    public static AbstractInsnNode findFactoryRedirectThrowString(Target target, AbstractInsnNode start) {
        for (ListIterator<AbstractInsnNode> it = target.insns.iterator(target.indexOf(start)); it.hasNext(); ) {
            AbstractInsnNode insn = it.next();

            if (insn instanceof LdcInsnNode) {
                LdcInsnNode ldc = (LdcInsnNode) insn;
                if (ldc.cst instanceof String && ((String) ldc.cst).startsWith("@Redirect constructor handler ")) {
                    return ldc;
                }
            }
        }
        LOGGER.warn(
                "Please inform LlamaLad7! Failed to find factory redirect throw string for {}",
                Bytecode.describeNode(start)
        );
        return null;
    }

    public static void checkForImmediatePops(Map<Target, List<InjectionNode>> targets) {
        for (List<InjectionNodes.InjectionNode> nodeList : targets.values()) {
            for (InjectionNodes.InjectionNode node : nodeList) {
                AbstractInsnNode currentTarget = node.getCurrentTarget();
                if (currentTarget instanceof MethodInsnNode) {
                    Type returnType = Type.getReturnType(((MethodInsnNode) currentTarget).desc);
                    if (isTypePoppedByInstruction(returnType, currentTarget.getNext())) {
                        node.decorate(Decorations.POPPED_OPERATION, true);
                    }
                }
            }
        }
    }

    private static boolean isTypePoppedByInstruction(Type type, AbstractInsnNode insn) {
        switch (type.getSize()) {
            case 2:
                return insn.getOpcode() == Opcodes.POP2;
            case 1:
                return insn.getOpcode() == Opcodes.POP;
            default:
                return false;
        }
    }

    public static Context getOrCreateLocalContext(Target target, InjectionNode node, InjectionInfo info, Type targetType, boolean isArgsOnly) {
        String decorationKey = getLocalContextKey(targetType, isArgsOnly);
        if (node.hasDecoration(decorationKey)) {
            return node.getDecoration(decorationKey);
        }
        Context context = CompatibilityHelper.makeLvtContext(info, targetType, isArgsOnly, target, node.getCurrentTarget());
        node.decorate(decorationKey, context);
        return context;
    }

    private static String getLocalContextKey(Type targetType, boolean isArgsOnly) {
        return String.format(Decorations.PERSISTENT + "localContext(%s,%s)", targetType, isArgsOnly ? "argsOnly" : "fullFrame");
    }

    public static void printLocals(Target target, AbstractInsnNode node, Context context, LocalVariableDiscriminator discriminator, Type targetType, boolean isArgsOnly) {
        int baseArgIndex = target.isStatic ? 0 : 1;

        new PrettyPrinter()
                .kvWidth(20)
                .kv("Target Class", target.classNode.name.replace('/', '.'))
                .kv("Target Method", target.method.name)
                .kv("Capture Type", SignaturePrinter.getTypeName(targetType, false))
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

    private static boolean isImplicit(LocalVariableDiscriminator discriminator, int baseArgIndex) {
        return discriminator.getOrdinal() < 0 && discriminator.getIndex() < baseArgIndex && discriminator.getNames().isEmpty();
    }
}
