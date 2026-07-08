package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ExpressionSource;
import com.llamalad7.mixinextras.expression.impl.MatchResult;
import com.llamalad7.mixinextras.expression.impl.ast.Argument;
import com.llamalad7.mixinextras.expression.impl.ast.identifiers.TypeIdentifier;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.flow.postprocessing.ArrayCreationInfo;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;
import com.llamalad7.mixinextras.expression.impl.utils.FlowDecorations;
import com.llamalad7.mixinextras.expression.impl.utils.ExpressionASMUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.util.List;

public class ArrayLiteralExpression extends SimpleExpression {
    public final TypeIdentifier elementType;
    public final List<Argument> values;

    public ArrayLiteralExpression(ExpressionSource src, TypeIdentifier elementType, List<Argument> values) {
        super(src);
        this.elementType = elementType;
        this.values = values;
    }

    @Override
    protected MatchResult matchImpl(FlowValue node, ExpressionContext ctx) {
        ArrayCreationInfo creation = node.getDecoration(FlowDecorations.ARRAY_CREATION_INFO);
        if (creation == null) {
            return MatchResult.FAILURE;
        }
        Type newElementType = getElementType(node.getInsn());
        if (newElementType == null || !elementType.matches(ctx.pool, newElementType)) {
            return MatchResult.FAILURE;
        }
        return matchArguments(node, ctx, values);
    }

    private Type getElementType(AbstractInsnNode insn) {
        switch (insn.getOpcode()) {
            case Opcodes.ANEWARRAY:
                return Type.getObjectType(((TypeInsnNode) insn).desc);
            case Opcodes.NEWARRAY:
                return ExpressionASMUtils.getNewArrayType((IntInsnNode) insn);
        }
        return null;
    }
}
