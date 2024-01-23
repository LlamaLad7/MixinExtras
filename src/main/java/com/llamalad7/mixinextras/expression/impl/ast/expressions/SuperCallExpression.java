package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ast.identifiers.MemberIdentifier;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodInsnNode;

import java.util.List;

public class SuperCallExpression implements SimpleExpression {
    public final MemberIdentifier name;
    public final List<Expression> arguments;

    public SuperCallExpression(MemberIdentifier name, List<Expression> arguments) {
        this.name = name;
        this.arguments = arguments;
    }

    @Override
    public boolean matches(FlowValue node, ExpressionContext ctx) {
        if (ctx.getTarget().isStatic || node.getInsn().getOpcode() != Opcodes.INVOKESPECIAL) {
            return false;
        }
        MethodInsnNode call = (MethodInsnNode) node.getInsn();
        if (call.name.equals("<init>") || !call.owner.equals(ctx.getTarget().classNode.superName)) {
            return false;
        }
        if (!name.matches(ctx.getPool(), node.getInsn())) {
            return false;
        }
        return new ThisExpression().matches(node.getInput(0), ctx) && inputsMatch(1, node, ctx, arguments.toArray(new Expression[0]));
    }
}
