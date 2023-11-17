package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool;
import org.objectweb.asm.Opcodes;

public class ArrayStoreExpression implements Expression {
    private static final long serialVersionUID = -3433941561737164358L;

    public final Expression arr;
    public final Expression index;
    public final Expression value;

    public ArrayStoreExpression(Expression arr, Expression index, Expression value) {
        this.arr = arr;
        this.index = index;
        this.value = value;
    }

    @Override
    public boolean matches(FlowValue node, IdentifierPool pool, OutputSink sink) {
        switch (node.getInsn().getOpcode()) {
            case Opcodes.IASTORE:
            case Opcodes.LASTORE:
            case Opcodes.FASTORE:
            case Opcodes.DASTORE:
            case Opcodes.AASTORE:
            case Opcodes.BASTORE:
            case Opcodes.CASTORE:
            case Opcodes.SASTORE:
                return inputsMatch(node, pool, sink, arr, index, value);
        }
        return false;
    }
}
