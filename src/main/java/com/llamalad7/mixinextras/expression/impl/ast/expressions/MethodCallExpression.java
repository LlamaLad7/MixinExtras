package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ast.identifiers.Identifier;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;
import org.apache.commons.lang3.ArrayUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodInsnNode;

import java.util.List;

public class MethodCallExpression implements SimpleExpression {
    public final Expression receiver;
    public final Identifier name;
    public final List<Expression> arguments;

    public MethodCallExpression(Expression receiver, Identifier name, List<Expression> arguments) {
        this.receiver = receiver;
        this.name = name;
        this.arguments = arguments;
    }

    @Override
    public boolean matches(FlowValue node, ExpressionContext ctx) {
        switch (node.getInsn().getOpcode()) {
            case Opcodes.INVOKESPECIAL:
                MethodInsnNode call = (MethodInsnNode) node.getInsn();
                if (call.name.equals("<init>") || !call.owner.equals(ctx.getTarget().classNode.name)) {
                    return false;
                }
            case Opcodes.INVOKEVIRTUAL:
            case Opcodes.INVOKEINTERFACE:
                if (!name.matches(ctx.getPool(), node.getInsn(), Identifier.Role.MEMBER)) {
                    return false;
                }
                Expression[] inputs = ArrayUtils.add(arguments.toArray(new Expression[0]), 0, receiver);
                return inputsMatch(node, ctx, inputs);
        }
        return false;
    }
}
