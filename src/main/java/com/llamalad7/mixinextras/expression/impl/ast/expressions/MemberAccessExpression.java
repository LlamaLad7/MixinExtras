package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ast.identifiers.Identifier;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;

public class MemberAccessExpression implements SimpleExpression {
    private static final long serialVersionUID = 6076770192300216063L;

    public final Expression receiver;
    public final Identifier name;

    public MemberAccessExpression(Expression receiver, Identifier name) {
        this.receiver = receiver;
        this.name = name;
    }

    @Override
    public boolean matches(FlowValue node, IdentifierPool pool, OutputSink sink) {
        AbstractInsnNode insn = node.getInsn();
        return insn.getOpcode() == Opcodes.GETFIELD && name.matches(pool, insn) && inputsMatch(node, pool, sink, receiver);
    }
}
