package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ExpressionSource;
import com.llamalad7.mixinextras.expression.impl.ast.identifiers.MemberIdentifier;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;
import org.apache.commons.lang3.ArrayUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodInsnNode;

import java.util.List;

public class MethodCallExpression extends SimpleExpression {
    public final Expression receiver;
    public final MemberIdentifier name;
    public final List<Expression> arguments;

    public MethodCallExpression(ExpressionSource src, Expression receiver, MemberIdentifier name, List<Expression> arguments) {
        super(src);
        this.receiver = receiver;
        this.name = name;
        this.arguments = arguments;
    }

    @Override
    public boolean matches(FlowValue node, ExpressionContext ctx) {
        switch (node.getInsn().getOpcode()) {
            case Opcodes.INVOKESPECIAL:
                MethodInsnNode call = (MethodInsnNode) node.getInsn();
                if (call.name.equals("<init>") || !call.owner.equals(ctx.classNode.name)) {
                    return false;
                }
            case Opcodes.INVOKEVIRTUAL:
            case Opcodes.INVOKEINTERFACE:
                if (!name.matches(ctx.pool, node.getInsn())) {
                    return false;
                }
                Expression[] inputs = ArrayUtils.add(arguments.toArray(new Expression[0]), 0, receiver);
                return inputsMatch(node, ctx, ctx.allowIncompleteListInputs, inputs);
        }
        return false;
    }
}
