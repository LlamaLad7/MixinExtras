package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ExpressionSource;
import com.llamalad7.mixinextras.expression.impl.ast.identifiers.MemberIdentifier;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;

public class MemberAssignmentExpression extends Expression {
    public final Expression receiver;
    public final MemberIdentifier name;
    public final Expression value;

    public MemberAssignmentExpression(ExpressionSource src, Expression receiver, MemberIdentifier name, Expression value) {
        super(src);
        this.receiver = receiver;
        this.name = name;
        this.value = value;
    }

    @Override
    protected boolean matchesImpl(FlowValue node, ExpressionContext ctx) {
        AbstractInsnNode insn = node.getInsn();
        return insn.getOpcode() == Opcodes.PUTFIELD
                && name.matches(ctx.pool, node) && inputsMatch(node, ctx, receiver, value);
    }
}
