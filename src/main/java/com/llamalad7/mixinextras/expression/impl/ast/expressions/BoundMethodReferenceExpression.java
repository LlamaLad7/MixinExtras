package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ExpressionSource;
import com.llamalad7.mixinextras.expression.impl.ast.identifiers.MemberIdentifier;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

public class BoundMethodReferenceExpression extends MethodReferenceExpression {
    public final Expression receiver;
    public final MemberIdentifier name;

    public BoundMethodReferenceExpression(ExpressionSource src, Expression receiver, MemberIdentifier name) {
        super(src);
        this.receiver = receiver;
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
                if (node.inputCount() == 0) {
                    return false;
                }
                return name.matches(ctx.pool, impl) && receiver.matches(node.getInput(0), ctx);
        }
        return false;
    }
}
