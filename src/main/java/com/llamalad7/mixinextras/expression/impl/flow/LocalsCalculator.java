package com.llamalad7.mixinextras.expression.impl.flow;

import com.llamalad7.mixinextras.utils.ASMUtils;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Interpreter;
import org.spongepowered.asm.util.asm.ASM;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.*;

import static org.objectweb.asm.Opcodes.*;

@SuppressWarnings("unchecked")
class LocalsCalculator extends Interpreter<BasicValue> {
    private final Map<VarInsnNode, Object> results = new IdentityHashMap<>();
    private final MethodNode methodNode;

    public static Map<VarInsnNode, Type> getLocalTypes(ClassNode classNode, MethodNode methodNode) {
        LocalsCalculator calculator = new LocalsCalculator(methodNode);
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
                entry.setValue(((Set<Type>) entry.getValue()).stream().reduce(ASMUtils::getCommonSupertype).get());
            }
        }
        return (Map<VarInsnNode, Type>) (Object) calculator.results;
    }

    private LocalsCalculator(MethodNode methodNode) {
        super(ASM.API_VERSION);
        this.methodNode = methodNode;
    }

    @Override
    public BasicValue newValue(Type type) {
        if (type == Type.VOID_TYPE) {
            return null;
        }
        if (type == null) {
            type = ASMUtils.BOTTOM_TYPE;
        }
        return new BasicValue(type);
    }

    @Override
    public BasicValue newOperation(final AbstractInsnNode insn) {
        Type type;
        switch (insn.getOpcode()) {
            case ACONST_NULL:
                type = ASMUtils.BOTTOM_TYPE;
                break;
            case ICONST_M1:
            case ICONST_0:
            case ICONST_1:
            case ICONST_2:
            case ICONST_3:
            case ICONST_4:
            case ICONST_5:
            case BIPUSH:
            case SIPUSH:
                type = Type.INT_TYPE;
                break;
            case LCONST_0:
            case LCONST_1:
                type = Type.LONG_TYPE;
                break;
            case FCONST_0:
            case FCONST_1:
            case FCONST_2:
                type = Type.FLOAT_TYPE;
                break;
            case DCONST_0:
            case DCONST_1:
                type = Type.DOUBLE_TYPE;
                break;
            case LDC:
                Object cst = ((LdcInsnNode) insn).cst;
                if (cst instanceof Integer) {
                    type = Type.INT_TYPE;
                    break;
                }
                if (cst instanceof Float) {
                    type = Type.FLOAT_TYPE;
                    break;
                }
                if (cst instanceof Long) {
                    type = Type.LONG_TYPE;
                    break;
                }
                if (cst instanceof Double) {
                    type = Type.DOUBLE_TYPE;
                    break;
                }
                if (cst instanceof String) {
                    type = Type.getType(String.class);
                    break;
                }
                if (cst instanceof Type) {
                    int sort = ((Type) cst).getSort();
                    if (sort == Type.OBJECT || sort == Type.ARRAY) {
                        type = Type.getType(Class.class);
                        break;
                    }
                    if (sort == Type.METHOD) {
                        type = Type.getType(MethodType.class);
                        break;
                    }
                }
                if (cst instanceof Handle) {
                    type = Type.getType(MethodHandle.class);
                    break;
                }
                throw new IllegalArgumentException("Illegal LDC constant "
                        + cst);
            case GETSTATIC:
                type = Type.getType(((FieldInsnNode) insn).desc);
                break;
            case NEW:
                type = Type.getObjectType(((TypeInsnNode) insn).desc);
                break;
            default:
                throw new Error("Internal error.");
        }
        return new BasicValue(type);
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
        Type type;
        switch (insn.getOpcode()) {
            case INEG:
            case L2I:
            case F2I:
            case D2I:
            case ARRAYLENGTH:
            case IINC:
                type = Type.INT_TYPE;
                break;
            case I2B:
                type = Type.BYTE_TYPE;
                break;
            case I2C:
                type = Type.CHAR_TYPE;
                break;
            case I2S:
                type = Type.SHORT_TYPE;
                break;
            case FNEG:
            case I2F:
            case L2F:
            case D2F:
                type = Type.FLOAT_TYPE;
                break;
            case LNEG:
            case I2L:
            case F2L:
            case D2L:
                type = Type.LONG_TYPE;
                break;
            case DNEG:
            case I2D:
            case L2D:
            case F2D:
                type = Type.DOUBLE_TYPE;
                break;
            case IFEQ:
            case IFNE:
            case IFLT:
            case IFGE:
            case IFGT:
            case IFLE:
            case TABLESWITCH:
            case LOOKUPSWITCH:
            case IRETURN:
            case LRETURN:
            case FRETURN:
            case DRETURN:
            case ARETURN:
            case PUTSTATIC:
            case ATHROW:
            case MONITORENTER:
            case MONITOREXIT:
            case IFNULL:
            case IFNONNULL:
                type = Type.VOID_TYPE;
                break;
            case GETFIELD:
                type = Type.getType(((FieldInsnNode) insn).desc);
                break;
            case NEWARRAY:
                switch (((IntInsnNode) insn).operand) {
                    case T_BOOLEAN:
                        type = Type.getType("[Z");
                        break;
                    case T_CHAR:
                        type = Type.getType("[C");
                        break;
                    case T_BYTE:
                        type = Type.getType("[B");
                        break;
                    case T_SHORT:
                        type = Type.getType("[S");
                        break;
                    case T_INT:
                        type = Type.getType("[I");
                        break;
                    case T_FLOAT:
                        type = Type.getType("[F");
                        break;
                    case T_DOUBLE:
                        type = Type.getType("[D");
                        break;
                    case T_LONG:
                        type = Type.getType("[J");
                        break;
                    default:
                        throw new Error("Invalid array type " + ((IntInsnNode) insn).operand);
                }
                break;
            case ANEWARRAY:
                String desc = ((TypeInsnNode) insn).desc;
                type = Type.getType("[" + Type.getObjectType(desc));
                break;
            case CHECKCAST:
                desc = ((TypeInsnNode) insn).desc;
                type = Type.getObjectType(desc);
                break;
            case INSTANCEOF:
                type = Type.BOOLEAN_TYPE;
                break;
            default:
                throw new Error("Internal error.");
        }
        return new BasicValue(type);
    }

    @Override
    public BasicValue binaryOperation(
            final AbstractInsnNode insn, final BasicValue value1, final BasicValue value2) {
        Type type;
        switch (insn.getOpcode()) {
            case LALOAD:
            case LADD:
            case LSUB:
            case LMUL:
            case LDIV:
            case LREM:
            case LSHL:
            case LSHR:
            case LUSHR:
            case LAND:
            case LOR:
            case LXOR:
                type = Type.LONG_TYPE;
                break;
            case DALOAD:
            case DADD:
            case DSUB:
            case DMUL:
            case DDIV:
            case DREM:
                type = Type.DOUBLE_TYPE;
                break;
            case IALOAD:
            case IADD:
            case ISUB:
            case IMUL:
            case IDIV:
            case IREM:
            case ISHL:
            case ISHR:
            case IUSHR:
            case IAND:
            case IOR:
            case IXOR:
            case LCMP:
            case FCMPL:
            case FCMPG:
            case DCMPL:
            case DCMPG:
                type = Type.INT_TYPE;
                break;
            case FALOAD:
            case FADD:
            case FSUB:
            case FMUL:
            case FDIV:
            case FREM:
                type = Type.FLOAT_TYPE;
                break;
            case CALOAD:
                type = Type.CHAR_TYPE;
                break;
            case SALOAD:
                type = Type.SHORT_TYPE;
                break;
            case IF_ICMPEQ:
            case IF_ICMPNE:
            case IF_ICMPLT:
            case IF_ICMPGE:
            case IF_ICMPGT:
            case IF_ICMPLE:
            case IF_ACMPEQ:
            case IF_ACMPNE:
            case PUTFIELD:
                type = Type.VOID_TYPE;
                break;
            case AALOAD:
            case BALOAD:
                type = ASMUtils.getInnerType(value1.getType());
                break;
            default:
                throw new Error("Internal error.");
        }
        return new BasicValue(type);
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
        int opcode = insn.getOpcode();
        Type type;
        switch (opcode) {
            case MULTIANEWARRAY:
                type = Type.getType(((MultiANewArrayInsnNode) insn).desc);
                break;
            case INVOKEDYNAMIC:
                type = Type.getReturnType(((InvokeDynamicInsnNode) insn).desc);
                break;
            default:
                type = Type.getReturnType(((MethodInsnNode) insn).desc);
                break;
        }
        return new BasicValue(type);
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
        return new BasicValue(ASMUtils.getCommonSupertype(value1.getType(), value2.getType()));
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
