package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool;
import org.objectweb.asm.Opcodes;

public class ArrayAccessExpression implements SimpleExpression {
    private static final long serialVersionUID = -2463724143455495283L;

    public final Expression arr;
    public final Expression index;

    public ArrayAccessExpression(Expression arr, Expression index) {
        this.arr = arr;
        this.index = index;
    }

    @Override
    public boolean matches(FlowValue node, IdentifierPool pool, OutputSink sink) {
        switch (node.getInsn().getOpcode()) {
            case Opcodes.IALOAD:
            case Opcodes.LALOAD:
            case Opcodes.FALOAD:
            case Opcodes.DALOAD:
            case Opcodes.AALOAD:
            case Opcodes.BALOAD:
            case Opcodes.CALOAD:
            case Opcodes.SALOAD:
                return inputsMatch(node, pool, sink, arr, index);
        }
        return false;
    }
}
