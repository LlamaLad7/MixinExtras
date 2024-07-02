package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ExpressionSource;
import com.llamalad7.mixinextras.expression.impl.ast.identifiers.TypeIdentifier;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class ConstructorReferenceExpression extends MethodReferenceExpression {
    public final TypeIdentifier type;

    public ConstructorReferenceExpression(ExpressionSource src, TypeIdentifier type) {
        super(src);
        this.type = type;
    }

    @Override
    public boolean matches(FlowValue node, Handle impl, ExpressionContext ctx) {
        if (impl.getTag() != Opcodes.H_NEWINVOKESPECIAL || node.inputCount() != 0) {
            return false;
        }
        return type.matches(ctx.pool, Type.getObjectType(impl.getOwner()));
    }
}
