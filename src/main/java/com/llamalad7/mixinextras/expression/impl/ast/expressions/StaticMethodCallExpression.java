package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ast.identifiers.Identifier;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;

import java.util.List;

public class StaticMethodCallExpression implements SimpleExpression {
    public final Identifier name;
    public final List<Expression> arguments;

    public StaticMethodCallExpression(Identifier name, List<Expression> arguments) {
        this.name = name;
        this.arguments = arguments;
    }

    @Override
    public boolean matches(FlowValue node, ExpressionContext ctx) {
        AbstractInsnNode insn = node.getInsn();
        return insn.getOpcode() == Opcodes.INVOKESTATIC
                && name.matches(ctx.getPool(), insn, Identifier.Role.MEMBER) && inputsMatch(node, ctx, arguments.toArray(new Expression[0]));
    }
}
