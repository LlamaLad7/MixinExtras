package com.llamalad7.mixinextras.expression.impl.flow;

import com.llamalad7.mixinextras.expression.impl.utils.ExpressionASMUtils;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Interpreter;
import org.spongepowered.asm.util.asm.ASM;

import java.util.*;

import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ILOAD;

@SuppressWarnings("unchecked")
class LocalsCalculator extends Interpreter<BasicValue> {
    private final Map<VarInsnNode, Object> results = new IdentityHashMap<>();
    private final MethodNode methodNode;
    private final FlowContext context;

    public static Map<VarInsnNode, Type> getLocalTypes(ClassNode classNode, MethodNode methodNode, FlowContext ctx) {
        LocalsCalculator calculator = new LocalsCalculator(methodNode, ctx);
        try {
            new Analyzer<>(calculator).analyze(classNode.name, methodNode);
        } catch (AnalyzerException e) {
            throw new RuntimeException(
                    String.format(
                            "Failed to calculate locals for %s::%s%s: ",
                            classNode.name, methodNode.name, methodNode.desc
                    ),
                    e
            );
        }
        for (Map.Entry<VarInsnNode, Object> entry : calculator.results.entrySet()) {
            if (entry.getValue() instanceof Set) {
                entry.setValue(((Set<Type>) entry.getValue()).stream().reduce((type1, type2) -> ExpressionASMUtils.getCommonSupertype(ctx, type1, type2)).get());
            }
        }
        return (Map<VarInsnNode, Type>) (Object) calculator.results;
    }

    private LocalsCalculator(MethodNode methodNode, FlowContext ctx) {
        super(ASM.API_VERSION);
        this.methodNode = methodNode;
        this.context = ctx;
    }

    @Override
    public BasicValue newValue(Type type) {
        if (type == Type.VOID_TYPE) {
            return null;
        }
        if (type == null) {
            type = ExpressionASMUtils.BOTTOM_TYPE;
        }
        return new BasicValue(type);
    }

    @Override
    public BasicValue newOperation(final AbstractInsnNode insn) {
        return new BasicValue(ExpressionASMUtils.getNewType(insn));
    }

    @Override
    public BasicValue copyOperation(final AbstractInsnNode insn, BasicValue value) {
        if (insn.getOpcode() >= ILOAD && insn.getOpcode() <= ALOAD) {
            VarInsnNode varNode = (VarInsnNode) insn;
            recordType(varNode, value.getType());
        }
        return value;
    }

    @Override
    public BasicValue unaryOperation(final AbstractInsnNode insn, final BasicValue value) {
        return new BasicValue(ExpressionASMUtils.getUnaryType(insn));
    }

    @Override
    public BasicValue binaryOperation(
            final AbstractInsnNode insn, final BasicValue value1, final BasicValue value2) {
        return new BasicValue(ExpressionASMUtils.getBinaryType(insn, value1.getType()));
    }

    @Override
    public BasicValue ternaryOperation(
            final AbstractInsnNode insn,
            final BasicValue value1,
            final BasicValue value2,
            final BasicValue value3) {
        return null;
    }

    @Override
    public BasicValue naryOperation(
            final AbstractInsnNode insn, final List<? extends BasicValue> values) {
        return new BasicValue(ExpressionASMUtils.getNaryType(insn));
    }

    @Override
    public void returnOperation(
            final AbstractInsnNode insn, final BasicValue value, final BasicValue expected) {
        // Nothing to do.
    }

    @Override
    public BasicValue merge(final BasicValue value1, final BasicValue value2) {
        if (value1.equals(value2)) {
            return value1;
        }
        return new BasicValue(ExpressionASMUtils.getCommonSupertype(context, value1.getType(), value2.getType()));
    }

    private void recordType(VarInsnNode insn, Type type) {
        Object cached = results.get(insn);
        if (cached instanceof Type) {
            return;
        }
        if (cached instanceof Set) {
            ((Set<Type>) cached).add(type);
        }
        LocalVariableNode local = getLocalVariableAt(insn);
        results.put(insn, local != null ? Type.getType(local.desc) : new HashSet<>(Collections.singleton(type)));
    }

    private LocalVariableNode getLocalVariableAt(VarInsnNode varInsn) {
        int pos = methodNode.instructions.indexOf(varInsn);
        int var = varInsn.var;

        if (methodNode.localVariables == null || methodNode.localVariables.isEmpty()) {
            return null;
        }
        LocalVariableNode localVariableNode = null;

        for (LocalVariableNode local : methodNode.localVariables) {
            if (local.index != var || local.desc == null) {
                continue;
            }
            if (isOpcodeInRange(methodNode.instructions, local, pos)) {
                localVariableNode = local;
            }
        }

        return localVariableNode;
    }

    private boolean isOpcodeInRange(InsnList insns, LocalVariableNode local, int pos) {
        return insns.indexOf(local.start) <= pos && insns.indexOf(local.end) > pos;
    }
}
