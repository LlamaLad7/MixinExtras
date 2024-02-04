package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ExpressionSource;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;
import com.llamalad7.mixinextras.utils.Decorations;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class ArrayAccessExpression extends SimpleExpression {
    public final Expression arr;
    public final Expression index;

    public ArrayAccessExpression(ExpressionSource src, Expression arr, Expression index) {
        super(src);
        this.arr = arr;
        this.index = index;
    }

    @Override
    public boolean matches(FlowValue node, ExpressionContext ctx) {
        switch (node.getInsn().getOpcode()) {
            case Opcodes.IALOAD:
            case Opcodes.LALOAD:
            case Opcodes.FALOAD:
            case Opcodes.DALOAD:
            case Opcodes.AALOAD:
            case Opcodes.BALOAD:
            case Opcodes.CALOAD:
            case Opcodes.SALOAD:
                return inputsMatch(node, ctx, arr, index);
        }
        return false;
    }

    @Override
    public void capture(FlowValue node, OutputSink sink) {
        sink.decorate(node.getInsn(), Decorations.SIMPLE_OPERATION_ARGS, new Type[]{node.getInput(0).getType(), Type.INT_TYPE});
        sink.decorate(node.getInsn(), Decorations.SIMPLE_OPERATION_RETURN_TYPE, node.getType());
        super.capture(node, sink);
    }
}
