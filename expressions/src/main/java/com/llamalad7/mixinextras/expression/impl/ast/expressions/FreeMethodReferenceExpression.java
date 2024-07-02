package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ExpressionSource;
import com.llamalad7.mixinextras.expression.impl.ast.identifiers.MemberIdentifier;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

public class FreeMethodReferenceExpression extends MethodReferenceExpression {
    public final MemberIdentifier name;

    public FreeMethodReferenceExpression(ExpressionSource src, MemberIdentifier name) {
        super(src);
        this.name = name;
    }

    @Override
    public boolean matches(FlowValue node, Handle impl, ExpressionContext ctx) {
        switch (impl.getTag()) {
            case Opcodes.H_INVOKESPECIAL:
                if (!impl.getOwner().equals(ctx.classNode.name)) {
                    return false;
                }
            case Opcodes.H_INVOKEVIRTUAL:
            case Opcodes.H_INVOKEINTERFACE:
                if (node.inputCount() != 0) {
                    return false;
                }
            case Opcodes.H_INVOKESTATIC:
                return name.matches(ctx.pool, impl);
        }
        return false;
    }
}
