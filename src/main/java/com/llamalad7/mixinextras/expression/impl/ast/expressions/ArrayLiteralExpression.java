package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ExpressionSource;
import com.llamalad7.mixinextras.expression.impl.ast.identifiers.TypeIdentifier;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.flow.postprocessing.ArrayCreationInfo;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;
import com.llamalad7.mixinextras.utils.Decorations;
import com.llamalad7.mixinextras.utils.TypeUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.util.List;

public class ArrayLiteralExpression extends SimpleExpression {
    public final TypeIdentifier elementType;
    public final List<Expression> values;

    public ArrayLiteralExpression(ExpressionSource src, TypeIdentifier elementType, List<Expression> values) {
        super(src);
        this.elementType = elementType;
        this.values = values;
    }

    @Override
    public boolean matches(FlowValue node, ExpressionContext ctx) {
        ArrayCreationInfo creation = node.getDecoration(Decorations.ARRAY_CREATION_INFO);
        if (creation == null) {
            return false;
        }
        Type newElementType = getElementType(node.getInsn());
        if (newElementType == null || !elementType.matches(ctx.pool, newElementType)) {
            return false;
        }
        return inputsMatch(node, ctx, ctx.allowIncompleteListInputs, values.toArray(new Expression[0]));
    }

    private Type getElementType(AbstractInsnNode insn) {
        switch (insn.getOpcode()) {
            case Opcodes.ANEWARRAY:
                return Type.getObjectType(((TypeInsnNode) insn).desc);
            case Opcodes.NEWARRAY:
                return TypeUtils.getNewArrayType((IntInsnNode) insn);
        }
        return null;
    }
}
