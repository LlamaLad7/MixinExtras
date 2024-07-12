package com.llamalad7.mixinextras.expression.impl.flow.expansion;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.flow.postprocessing.FlowPostProcessor;
import com.llamalad7.mixinextras.expression.impl.utils.ExpressionASMUtils;
import org.apache.commons.lang3.StringUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes;
import org.spongepowered.asm.mixin.injection.struct.Target;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

public class StringConcatFactoryExpander extends InsnExpander {
    private static final String STRING_CONCAT_FACTORY = "java/lang/invoke/StringConcatFactory";
    private static final Type STRING_BUILDER = Type.getType(StringBuilder.class);
    private static final Type STRING = Type.getType(String.class);

    @Override
    public void process(FlowValue node, FlowPostProcessor.OutputSink sink) {
        AbstractInsnNode indy = node.getInsn();
        List<ConcatPart> parts = parseConcat(indy);
        if (parts == null) {
            return;
        }

        FlowValue current = new FlowValue(STRING_BUILDER, makeNewBuilder());
        registerComponent(current, Component.NEW_BUILDER, indy);
        FlowValue initCall = new FlowValue(Type.VOID_TYPE, makeBuilderInit(), current);
        registerComponent(initCall, Component.BUILDER_INIT, indy);
        sink.registerFlow(current, initCall);

        int nextArgument = 0;
        int finishedParts = 0;
        for (ConcatPart part : parts) {
            FlowValue component;
            if (part instanceof ConcatPart.Argument) {
                component = node.getInput(nextArgument++);
            } else {
                Object cst;
                if (part instanceof ConcatPart.PooledConstant) {
                    cst = ((ConcatPart.PooledConstant) part).value;
                } else {
                    cst = ((ConcatPart.TemplateString) part).value;
                }
                AbstractInsnNode componentInsn = new LdcInsnNode(cst);
                component = new FlowValue(ExpressionASMUtils.getNewType(componentInsn), componentInsn);
                registerComponent(component, part, indy);
                sink.registerFlow(component);
            }
            current = new FlowValue(STRING_BUILDER, makeAppendCall(component.getType()), current, component);
            registerComponent(current, new PartialResult(finishedParts++), indy);
            sink.registerFlow(current);
        }

        node.setInsn(makeToStringCall());
        node.setParents(current);
        registerComponent(node, Component.TO_STRING, indy);
    }

    @Override
    public void expand(Target target, InjectionNodes.InjectionNode node, Expansion expansion) {
        InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) node.getCurrentTarget();
        Set<InsnComponent> interests = expansion.registeredInterests();
        if (interests.size() == 1 && interests.iterator().next() == Component.TO_STRING) {
            // Fast path, just leave as-is.
            expansion.registerInsn(Component.TO_STRING, node.getCurrentTarget());
            return;
        }
        List<AbstractInsnNode> insns = new ArrayList<>();
        Type[] argTypes = Type.getArgumentTypes(indy.desc);
        int[] argMap = storeArgs(target, argTypes, insns::add);

        insns.add(expansion.registerInsn(Component.NEW_BUILDER, makeNewBuilder()));
        insns.add(new InsnNode(Opcodes.DUP));
        target.method.maxStack += 2;
        insns.add(expansion.registerInsn(Component.BUILDER_INIT, makeBuilderInit()));

        int nextArgument = 0;
        int finishedParts = 0;
        for (ConcatPart part : parseConcat(indy)) {
            Type partType;
            if (part instanceof ConcatPart.Argument) {
                int arg = nextArgument++;
                partType = argTypes[arg];
                insns.add(new VarInsnNode(partType.getOpcode(Opcodes.ILOAD), argMap[arg]));
            } else {
                Object cst;
                if (part instanceof ConcatPart.PooledConstant) {
                    cst = ((ConcatPart.PooledConstant) part).value;
                } else {
                    cst = ((ConcatPart.TemplateString) part).value;
                }
                AbstractInsnNode componentInsn = expansion.registerInsn(part, new LdcInsnNode(cst));
                partType = ExpressionASMUtils.getNewType(componentInsn);
                target.method.maxStack += partType.getSize();
                insns.add(componentInsn);
            }
            insns.add(expansion.registerInsn(new PartialResult(finishedParts++), makeAppendCall(partType)));
        }

        insns.add(expansion.registerInsn(Component.TO_STRING, makeToStringCall()));

        expandInsn(target, node, insns.toArray(new AbstractInsnNode[0]));
    }

    private List<ConcatPart> parseConcat(AbstractInsnNode insn) {
        if (!(insn instanceof InvokeDynamicInsnNode)) {
            return null;
        }
        InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) insn;
        if (!indy.bsm.getOwner().equals(STRING_CONCAT_FACTORY)) {
            return null;
        }
        if (indy.bsm.getName().equals("makeConcat")) {
            int inputCount = Type.getArgumentTypes(indy.desc).length;
            return parseConcatWithConstants(new Object[]{StringUtils.repeat('\u0001', inputCount)});
        }
        if (indy.bsm.getName().equals("makeConcatWithConstants")) {
            return parseConcatWithConstants(indy.bsmArgs);
        }
        return null;
    }

    private AbstractInsnNode makeNewBuilder() {
        return new TypeInsnNode(Opcodes.NEW, STRING_BUILDER.getInternalName());
    }

    private AbstractInsnNode makeBuilderInit() {
        return new MethodInsnNode(
                Opcodes.INVOKESPECIAL,
                STRING_BUILDER.getInternalName(),
                "<init>",
                "()V",
                false
        );
    }

    private AbstractInsnNode makeAppendCall(Type type) {
        if (type.getSort() == Type.OBJECT) {
            type = ExpressionASMUtils.OBJECT_TYPE;
        }
        return new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                STRING_BUILDER.getInternalName(),
                "append",
                Type.getMethodDescriptor(STRING_BUILDER, type),
                false
        );
    }

    private AbstractInsnNode makeToStringCall() {
        return new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                STRING_BUILDER.getInternalName(),
                "toString",
                Type.getMethodDescriptor(STRING),
                false
        );
    }

    private List<ConcatPart> parseConcatWithConstants(Object[] bsmArgs) {
        String template = (String) bsmArgs[0];
        List<ConcatPart> result = new ArrayList<>();
        int id = 0;
        int nextCst = 1;
        StringBuilder currentString = null;
        for (int i = 0; i < template.length(); i++) {
            char c = template.charAt(i);
            if ((c == '\u0001' || c == '\u0002') && currentString != null) {
                result.add(new ConcatPart.TemplateString(id++, currentString.toString()));
                currentString = null;
            }
            switch (c) {
                case '\u0001':
                    result.add(new ConcatPart.Argument(id++));
                    break;
                case '\u0002':
                    result.add(new ConcatPart.PooledConstant(id++, bsmArgs[nextCst++]));
                    break;
                default:
                    if (currentString == null) {
                        currentString = new StringBuilder();
                    }
                    currentString.append(c);
            }
        }
        if (currentString != null) {
            result.add(new ConcatPart.TemplateString(id, currentString.toString()));
        }
        return result;
    }

    private int[] storeArgs(Target target, Type[] args, Consumer<AbstractInsnNode> add) {
        int[] map = new int[args.length];
        for (int i = args.length - 1; i >= 0; i--) {
            Type type = args[i];
            int index = target.allocateLocals(type.getSize());
            target.addLocalVariable(index, "concatTemp" + index, type.getDescriptor());
            map[i] = index;
            add.accept(new VarInsnNode(type.getOpcode(Opcodes.ISTORE), index));
        }
        return map;
    }

    private enum Component implements InsnComponent {
        NEW_BUILDER, BUILDER_INIT, TO_STRING
    }

    private abstract static class ConcatPart implements InsnComponent {
        private final int id;

        private ConcatPart(int id) {
            this.id = id;
        }

        public static class Argument extends ConcatPart {
            public Argument(int id) {
                super(id);
            }
        }

        public static class PooledConstant extends ConcatPart {
            public final Object value;

            public PooledConstant(int id, Object value) {
                super(id);
                this.value = value;
            }
        }

        public static class TemplateString extends ConcatPart {
            public final String value;

            public TemplateString(int id, String value) {
                super(id);
                this.value = value;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ConcatPart that = (ConcatPart) o;
            return id == that.id;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(id);
        }
    }

    private static class PartialResult implements InsnComponent {
        public final int finishedParts;

        private PartialResult(int finishedParts) {
            this.finishedParts = finishedParts;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PartialResult that = (PartialResult) o;
            return finishedParts == that.finishedParts;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(finishedParts);
        }
    }
}
