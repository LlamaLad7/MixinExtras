package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ast.identifiers.TypeIdentifier;
import com.llamalad7.mixinextras.expression.impl.flow.ArrayCreationInfo;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;
import com.llamalad7.mixinextras.utils.Decorations;
import com.llamalad7.mixinextras.utils.TypeUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.util.List;

public class ArrayLiteralExpression implements SimpleExpression {
    public final TypeIdentifier elementType;
    public final List<Expression> values;

    public ArrayLiteralExpression(TypeIdentifier elementType, List<Expression> values) {
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
        return valuesMatch(creation.values, ctx);
    }

    private boolean valuesMatch(List<FlowValue> flows, ExpressionContext ctx) {
        if (flows.size() != values.size()) {
            return false;
        }
        for (int i = 0; i < flows.size(); i++) {
            if (!values.get(i).matches(flows.get(i), ctx)) {
                return false;
            }
        }
        return true;
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
