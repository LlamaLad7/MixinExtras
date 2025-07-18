package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ExpressionSource;
import com.llamalad7.mixinextras.expression.impl.ast.identifiers.TypeIdentifier;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;
import com.llamalad7.mixinextras.expression.impl.utils.FlowDecorations;
import com.llamalad7.mixinextras.expression.impl.utils.ExpressionASMUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.util.List;

public class NewArrayExpression extends SimpleExpression {
    public final TypeIdentifier innerType;
    public final List<Expression> dims;
    public final int blankDims;

    public NewArrayExpression(ExpressionSource src, TypeIdentifier innerType, List<Expression> dims, int blankDims) {
        super(src);
        this.innerType = innerType;
        this.dims = dims;
        this.blankDims = blankDims;
    }

    @Override
    protected boolean matchesImpl(FlowValue node, ExpressionContext ctx) {
        if (node.hasDecoration(FlowDecorations.ARRAY_CREATION_INFO)) {
            // While a creation *is* involved, it's not the kind we're trying to target.
            return false;
        }
        Type newInnerType = getInnerType(node.getInsn());
        if (newInnerType == null) {
            return false;
        }
        int newBlankDims = getBlankDims(node.getInsn());
        if (newBlankDims + node.inputCount() < blankDims + dims.size()) {
            return false;
        }
        if (!innerType.matches(ctx.pool, newInnerType)) {
            return false;
        }
        return inputsMatch(node, ctx, ctx.allowIncompleteListInputs, dims.toArray(new Expression[0]));
    }

    private Type getInnerType(AbstractInsnNode insn) {
        switch (insn.getOpcode()) {
            case Opcodes.ANEWARRAY:
                Type elementType = Type.getObjectType(((TypeInsnNode) insn).desc);
                return elementType.getSort() == Type.ARRAY ? elementType.getElementType() : elementType;
            case Opcodes.NEWARRAY:
                return ExpressionASMUtils.getNewArrayType((IntInsnNode) insn);
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
