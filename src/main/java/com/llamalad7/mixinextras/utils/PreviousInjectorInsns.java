package com.llamalad7.mixinextras.utils;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public enum PreviousInjectorInsns {
    DYNAMIC_INSTANCEOF_REDIRECT("dynamic instanceof redirect") {
        @Override
        protected List<Predicate<AbstractInsnNode>> getPredicates() {
            return Arrays.asList(
                    it -> it.getOpcode() == Opcodes.DUP,
                    it -> it.getOpcode() == Opcodes.IFNONNULL,
                    it -> it.getOpcode() == Opcodes.NEW && ((TypeInsnNode) it).desc.equals(NPE),
                    it -> it.getOpcode() == Opcodes.DUP,
                    it -> isMessage(it, "@Redirect instanceof handler ", "@ModifyConstant instanceof handler "),
                    it -> it.getOpcode() == Opcodes.INVOKESPECIAL && ((MethodInsnNode) it).owner.equals(NPE),
                    it -> it.getOpcode() == Opcodes.ATHROW,
                    it -> it instanceof LabelNode,
                    it -> it.getOpcode() == Opcodes.SWAP,
                    it -> it.getOpcode() == Opcodes.DUP,
                    it -> it.getOpcode() == Opcodes.IFNULL,
                    it -> it.getOpcode() == Opcodes.INVOKEVIRTUAL && ((MethodInsnNode) it).name.equals("getClass"),
                    it -> it.getOpcode() == Opcodes.INVOKEVIRTUAL && ((MethodInsnNode) it).name.equals("isAssignableFrom"),
                    it -> it.getOpcode() == Opcodes.GOTO,
                    it -> it instanceof LabelNode,
                    it -> it.getOpcode() == Opcodes.POP,
                    it -> it.getOpcode() == Opcodes.POP,
                    it -> it.getOpcode() == Opcodes.ICONST_0,
                    it -> it instanceof LabelNode
            );
        }
    },
    DUPED_FACTORY_REDIRECT("duped factory redirect") {
        @Override
        protected List<Predicate<AbstractInsnNode>> getPredicates() {
            return Arrays.asList(
                    it -> it.getOpcode() == Opcodes.DUP,
                    it -> it.getOpcode() == Opcodes.IFNONNULL,
                    it -> it.getOpcode() == Opcodes.NEW && ((TypeInsnNode) it).desc.equals(NPE),
                    it -> it.getOpcode() == Opcodes.DUP,
                    it -> isMessage(it, "@Redirect constructor handler "),
                    it -> it.getOpcode() == Opcodes.INVOKESPECIAL && ((MethodInsnNode) it).owner.equals(NPE),
                    it -> it.getOpcode() == Opcodes.ATHROW,
                    it -> it instanceof LabelNode
            );
        }
    },
    COMPARISON_WRAPPER("comparison wrapper") {
        private final Predicate<AbstractInsnNode> is0Or1 =
                it -> it.getOpcode() == Opcodes.ICONST_0 || it.getOpcode() == Opcodes.ICONST_1;

        @Override
        protected List<Predicate<AbstractInsnNode>> getPredicates() {
            return Arrays.asList(
                    it -> it.getOpcode() == Opcodes.IFNE,
                    is0Or1,
                    it -> it.getOpcode() == Opcodes.GOTO,
                    it -> it instanceof LabelNode,
                    is0Or1,
                    it -> it instanceof LabelNode
            );
        }
    };

    private static final String NPE = Type.getInternalName(NullPointerException.class);

    private final String description;

    PreviousInjectorInsns(String description) {
        this.description = description;
    }

    protected abstract List<Predicate<AbstractInsnNode>> getPredicates();

    public void moveNodes(InsnList from, InsnList to, AbstractInsnNode node) {
        AbstractInsnNode current = node.getNext();
        for (Predicate<AbstractInsnNode> predicate : getPredicates()) {
            if (!predicate.test(current)) {
                throw new AssertionError(
                        String.format(
                                "Failed assertion when wrapping instructions of %s. Please inform LlamaLad7!",
                                description
                        )
                );
            }
            AbstractInsnNode old = current;
            do {
                current = current.getNext();
            } while (current instanceof FrameNode);
            from.remove(old);
            to.add(old);
        }
    }

    public AbstractInsnNode getLast(AbstractInsnNode node) {
        AbstractInsnNode current = node.getNext();
        AbstractInsnNode result = null;
        for (Predicate<AbstractInsnNode> predicate : getPredicates()) {
            if (!predicate.test(current)) {
                throw new AssertionError(
                        String.format(
                                "Failed assertion when walking instructions of %s. Please inform LlamaLad7!",
                                description
                        )
                );
            }
            result = current;
            do {
                current = current.getNext();
            } while (current instanceof FrameNode);
        }
        return result;
    }

    protected static boolean isMessage(AbstractInsnNode insn, String... messages) {
        if (!(insn instanceof LdcInsnNode)) {
            return false;
        }
        LdcInsnNode ldc = (LdcInsnNode) insn;
        if (!(ldc.cst instanceof String)) {
            return false;
        }
        String cst = (String) ldc.cst;
        return Arrays.stream(messages).anyMatch(cst::startsWith);
    }
}
