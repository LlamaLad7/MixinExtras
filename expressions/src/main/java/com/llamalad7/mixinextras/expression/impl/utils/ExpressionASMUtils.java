package com.llamalad7.mixinextras.expression.impl.utils;

import com.llamalad7.mixinextras.expression.impl.ExpressionService;
import com.llamalad7.mixinextras.expression.impl.flow.Boxing;
import com.llamalad7.mixinextras.expression.impl.flow.FlowContext;
import org.apache.commons.lang3.StringUtils;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.util.Bytecode;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import static org.objectweb.asm.Opcodes.*;

public class ExpressionASMUtils {
    public static final Type OBJECT_TYPE = Type.getType(Object.class);
    public static final Type BOTTOM_TYPE = Type.getObjectType("null");
    public static final Type INTLIKE_TYPE = Type.getObjectType("int-like");

    public static final Handle LMF_HANDLE = new Handle(
            Opcodes.H_INVOKESTATIC,
            "java/lang/invoke/LambdaMetafactory",
            "metafactory",
            Bytecode.generateDescriptor(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, MethodType.class, MethodHandle.class, MethodType.class),
            false
    );

    public static final Handle ALT_LMF_HANDLE = new Handle(
            Opcodes.H_INVOKESTATIC,
            "java/lang/invoke/LambdaMetafactory",
            "altMetafactory",
            Bytecode.generateDescriptor(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, Object[].class),
            false
    );

    public static Type getNewType(AbstractInsnNode insn) {
        switch (insn.getOpcode()) {
            case ACONST_NULL:
                return BOTTOM_TYPE;
            case ICONST_M1:
            case ICONST_0:
            case ICONST_1:
            case ICONST_2:
            case ICONST_3:
            case ICONST_4:
            case ICONST_5:
            case BIPUSH:
            case SIPUSH:
                return INTLIKE_TYPE;
            case LCONST_0:
            case LCONST_1:
                return Type.LONG_TYPE;
            case FCONST_0:
            case FCONST_1:
            case FCONST_2:
                return Type.FLOAT_TYPE;
            case DCONST_0:
            case DCONST_1:
                return Type.DOUBLE_TYPE;
            case LDC:
                Object cst = ((LdcInsnNode) insn).cst;
                if (cst instanceof Integer) {
                    return INTLIKE_TYPE;
                }
                if (cst instanceof Float) {
                    return Type.FLOAT_TYPE;
                }
                if (cst instanceof Long) {
                    return Type.LONG_TYPE;
                }
                if (cst instanceof Double) {
                    return Type.DOUBLE_TYPE;
                }
                if (cst instanceof String) {
                    return Type.getType(String.class);
                }
                if (cst instanceof Type) {
                    int sort = ((Type) cst).getSort();
                    if (sort == Type.OBJECT || sort == Type.ARRAY) {
                        return Type.getType(Class.class);
                    }
                    if (sort == Type.METHOD) {
                        return Type.getType(MethodType.class);
                    }
                }
                if (cst instanceof Handle) {
                    return Type.getType(MethodHandle.class);
                }
                throw new IllegalArgumentException("Illegal LDC constant "
                        + cst);
            case GETSTATIC:
                return Type.getType(((FieldInsnNode) insn).desc);
            case NEW:
                return Type.getObjectType(((TypeInsnNode) insn).desc);
            default:
                throw errorFor(insn);
        }
    }

    public static Type getUnaryType(AbstractInsnNode insn) {
        switch (insn.getOpcode()) {
            case INEG:
            case L2I:
            case F2I:
            case D2I:
            case ARRAYLENGTH:
            case IINC:
                return Type.INT_TYPE;
            case I2B:
                return Type.BYTE_TYPE;
            case I2C:
                return Type.CHAR_TYPE;
            case I2S:
                return Type.SHORT_TYPE;
            case FNEG:
            case I2F:
            case L2F:
            case D2F:
                return Type.FLOAT_TYPE;
            case LNEG:
            case I2L:
            case F2L:
            case D2L:
                return Type.LONG_TYPE;
            case DNEG:
            case I2D:
            case L2D:
            case F2D:
                return Type.DOUBLE_TYPE;
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
                return Type.VOID_TYPE;
            case GETFIELD:
                return Type.getType(((FieldInsnNode) insn).desc);
            case NEWARRAY:
                switch (((IntInsnNode) insn).operand) {
                    case T_BOOLEAN:
                        return Type.getType("[Z");
                    case T_CHAR:
                        return Type.getType("[C");
                    case T_BYTE:
                        return Type.getType("[B");
                    case T_SHORT:
                        return Type.getType("[S");
                    case T_INT:
                        return Type.getType("[I");
                    case T_FLOAT:
                        return Type.getType("[F");
                    case T_DOUBLE:
                        return Type.getType("[D");
                    case T_LONG:
                        return Type.getType("[J");
                    default:
                        throw new Error("Invalid array type " + ((IntInsnNode) insn).operand);
                }
            case ANEWARRAY:
                String desc = ((TypeInsnNode) insn).desc;
                return Type.getType("[" + Type.getObjectType(desc));
            case CHECKCAST:
                desc = ((TypeInsnNode) insn).desc;
                return Type.getObjectType(desc);
            case INSTANCEOF:
                return Type.BOOLEAN_TYPE;
            default:
                throw errorFor(insn);
        }
    }

    public static Type getBinaryType(AbstractInsnNode insn, Type left) {
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
                return Type.LONG_TYPE;
            case DALOAD:
            case DADD:
            case DSUB:
            case DMUL:
            case DDIV:
            case DREM:
                return Type.DOUBLE_TYPE;
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
                return Type.INT_TYPE;
            case FALOAD:
            case FADD:
            case FSUB:
            case FMUL:
            case FDIV:
            case FREM:
                return Type.FLOAT_TYPE;
            case CALOAD:
                return Type.CHAR_TYPE;
            case SALOAD:
                return Type.SHORT_TYPE;
            case IF_ICMPEQ:
            case IF_ICMPNE:
            case IF_ICMPLT:
            case IF_ICMPGE:
            case IF_ICMPGT:
            case IF_ICMPLE:
            case IF_ACMPEQ:
            case IF_ACMPNE:
            case PUTFIELD:
                return Type.VOID_TYPE;
            case AALOAD:
            case BALOAD:
                return getInnerType(left);
            default:
                throw errorFor(insn);
        }
    }

    public static Type getNaryType(AbstractInsnNode insn) {
        switch (insn.getOpcode()) {
            case MULTIANEWARRAY:
                return Type.getType(((MultiANewArrayInsnNode) insn).desc);
            case INVOKEDYNAMIC:
                return Type.getReturnType(((InvokeDynamicInsnNode) insn).desc);
            default:
                return Type.getReturnType(((MethodInsnNode) insn).desc);
        }
    }

    private static Error errorFor(AbstractInsnNode insn) {
        return new AssertionError(
                String.format(
                        "Could not compute type of %s! Please inform LlamaLad7!",
                        Bytecode.describeNode(insn)
                )
        );
    }

    public static Type getCommonSupertype(FlowContext ctx, Type type1, Type type2) {
        if (type1.equals(type2) || type2.equals(BOTTOM_TYPE)) {
            return type1;
        }
        if (type1.equals(BOTTOM_TYPE)) {
            return type2;
        }
        boolean isIntLike1 = isIntLike(type1);
        boolean isIntLike2 = isIntLike(type2);
        if (isIntLike1 && isIntLike2) {
            return INTLIKE_TYPE;
        }
        if (isIntLike1 || isIntLike2) {
            return BOTTOM_TYPE;
        }
        if (type1.getSort() == Type.ARRAY && type2.getSort() == Type.ARRAY) {
            int dim1 = type1.getDimensions();
            Type elem1 = type1.getElementType();
            int dim2 = type2.getDimensions();
            Type elem2 = type2.getElementType();
            if (dim1 == dim2) {
                Type commonSupertype;
                if (elem1.equals(elem2)) {
                    commonSupertype = elem1;
                } else if (elem1.getSort() == Type.OBJECT && elem2.getSort() == Type.OBJECT) {
                    commonSupertype = getCommonSupertype(ctx, elem1, elem2);
                } else {
                    return arrayType(OBJECT_TYPE, dim1 - 1);
                }
                return arrayType(commonSupertype, dim1);
            }
            Type smaller;
            int shared;
            if (dim1 < dim2) {
                smaller = elem1;
                shared = dim1 - 1;
            } else {
                smaller = elem2;
                shared = dim2 - 1;
            }
            if (smaller.getSort() == Type.OBJECT) {
                shared++;
            }
            return arrayType(OBJECT_TYPE, shared);
        }
        if (type1.getSort() == Type.ARRAY && type2.getSort() == Type.OBJECT || type2.getSort() == Type.ARRAY && type1.getSort() == Type.OBJECT) {
            return OBJECT_TYPE;
        }
        if (type1.getSort() != type2.getSort()) {
            return BOTTOM_TYPE;
        }
        return ExpressionService.getInstance().getCommonSuperClass(ctx, type1, type2);
    }

    public static Type getCommonIntType(FlowContext ctx, Type type1, Type type2) {
        Type unboxed1 = Boxing.getUnboxedType(type1.getInternalName());
        Type unboxed2 = Boxing.getUnboxedType(type2.getInternalName());

        return getCommonSupertype(
                ctx,
                unboxed1 != null ? unboxed1 : type1,
                unboxed2 != null ? unboxed2 : type2
        );
    }

    public static boolean isIntLike(Type type) {
        switch (type.getSort()) {
            case Type.BOOLEAN:
            case Type.CHAR:
            case Type.BYTE:
            case Type.SHORT:
            case Type.INT:
                return true;
            case Type.OBJECT:
                return type.equals(INTLIKE_TYPE);
        }
        return false;
    }

    private static Type arrayType(Type element, int dimensions) {
        return Type.getType(
                StringUtils.repeat('[', dimensions) + element.getDescriptor()
        );
    }

    public static Type getInnerType(Type arrayType) {
        if (arrayType.equals(BOTTOM_TYPE)) {
            return BOTTOM_TYPE;
        }
        return Type.getType(arrayType.getDescriptor().substring(1));
    }

    public static Type getNewArrayType(IntInsnNode newArray) {
        switch (newArray.operand) {
            case Opcodes.T_BOOLEAN:
                return Type.BOOLEAN_TYPE;
            case Opcodes.T_CHAR:
                return Type.CHAR_TYPE;
            case Opcodes.T_FLOAT:
                return Type.FLOAT_TYPE;
            case Opcodes.T_DOUBLE:
                return Type.DOUBLE_TYPE;
            case Opcodes.T_BYTE:
                return Type.BYTE_TYPE;
            case Opcodes.T_SHORT:
                return Type.SHORT_TYPE;
            case Opcodes.T_INT:
                return Type.INT_TYPE;
            case Opcodes.T_LONG:
                return Type.LONG_TYPE;
        }
        return null;
    }

    public static Object getConstant(AbstractInsnNode insn) {
        if (insn.getOpcode() == NEWARRAY) {
            // Mixin incorrectly throws when passed this.
            return null;
        }
        return Bytecode.getConstant(insn);
    }

    public static AbstractInsnNode pushInt(int integer) {
        switch (integer) {
            case -1:
                return new InsnNode(Opcodes.ICONST_M1);
            case 0:
                return new InsnNode(Opcodes.ICONST_0);
            case 1:
                return new InsnNode(Opcodes.ICONST_1);
            case 2:
                return new InsnNode(Opcodes.ICONST_2);
            case 3:
                return new InsnNode(Opcodes.ICONST_3);
            case 4:
                return new InsnNode(Opcodes.ICONST_4);
            case 5:
                return new InsnNode(Opcodes.ICONST_5);
        }
        if (Byte.MIN_VALUE <= integer && integer <= Byte.MAX_VALUE) {
            return new IntInsnNode(Opcodes.BIPUSH, integer);
        }
        if (Short.MIN_VALUE <= integer && integer <= Short.MAX_VALUE) {
            return new IntInsnNode(Opcodes.SIPUSH, integer);
        }
        return new LdcInsnNode(integer);
    }

    public static Type getCastType(AbstractInsnNode insn) {
        switch (insn.getOpcode()) {
            case Opcodes.CHECKCAST:
                return Type.getObjectType(((TypeInsnNode) insn).desc);
            case Opcodes.L2I:
            case Opcodes.F2I:
            case Opcodes.D2I:
                return Type.INT_TYPE;
            case Opcodes.I2B:
                return Type.BYTE_TYPE;
            case Opcodes.I2C:
                return Type.CHAR_TYPE;
            case Opcodes.I2S:
                return Type.SHORT_TYPE;
            case Opcodes.I2F:
            case Opcodes.L2F:
            case Opcodes.D2F:
                return Type.FLOAT_TYPE;
            case Opcodes.I2L:
            case Opcodes.F2L:
            case Opcodes.D2L:
                return Type.LONG_TYPE;
            case Opcodes.I2D:
            case Opcodes.L2D:
            case Opcodes.F2D:
                return Type.DOUBLE_TYPE;
        }
        return null;
    }
}
