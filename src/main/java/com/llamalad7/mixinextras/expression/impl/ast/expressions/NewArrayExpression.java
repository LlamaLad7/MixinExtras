package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ast.identifiers.TypeIdentifier;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;
import com.llamalad7.mixinextras.utils.Decorations;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.util.List;

public class NewArrayExpression implements SimpleExpression {
    public final TypeIdentifier innerType;
    public final List<Expression> dims;
    public final int blankDims;

    public NewArrayExpression(TypeIdentifier innerType, List<Expression> dims, int blankDims) {
        this.innerType = innerType;
        this.dims = dims;
        this.blankDims = blankDims;
    }

    @Override
    public boolean matches(FlowValue node, ExpressionContext ctx) {
        if (node.hasDecoration(Decorations.ARRAY_CREATION_INFO)) {
            // While a creation *is* involved, it's not the kind we're trying to target.
            return false;
        }
        Type newInnerType = getInnerType(node.getInsn());
        if (newInnerType == null) {
            return false;
        }
        int newBlankDims = getBlankDims(node.getInsn());
        if (blankDims != newBlankDims) {
            return false;
        }
        if (!innerType.matches(ctx.getPool(), newInnerType)) {
            return false;
        }
        return inputsMatch(node, ctx, dims.toArray(new Expression[0]));
    }

    private Type getInnerType(AbstractInsnNode insn) {
        switch (insn.getOpcode()) {
            case Opcodes.ANEWARRAY:
                Type elementType = Type.getObjectType(((TypeInsnNode) insn).desc);
                return elementType.getSort() == Type.ARRAY ? elementType.getElementType() : elementType;
            case Opcodes.NEWARRAY:
                int type = ((IntInsnNode) insn).operand;
                switch (type) {
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
            case Opcodes.MULTIANEWARRAY:
                return Type.getType(((MultiANewArrayInsnNode) insn).desc).getElementType();
        }
        return null;
    }

    private int getBlankDims(AbstractInsnNode insn) {
        switch (insn.getOpcode()) {
            case Opcodes.ANEWARRAY:
                Type elementType = Type.getObjectType(((TypeInsnNode) insn).desc);
                return elementType.getSort() == Type.ARRAY ? elementType.getDimensions() : 0;
            case Opcodes.MULTIANEWARRAY:
                MultiANewArrayInsnNode newArray = (MultiANewArrayInsnNode) insn;
                return Type.getType(newArray.desc).getDimensions() - newArray.dims;
        }
        return 0;
    }
}
