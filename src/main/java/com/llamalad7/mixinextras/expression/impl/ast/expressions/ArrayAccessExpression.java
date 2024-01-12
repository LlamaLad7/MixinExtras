package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool;
import com.llamalad7.mixinextras.expression.impl.serialization.ExpressionReader;
import com.llamalad7.mixinextras.expression.impl.serialization.ExpressionWriter;
import com.llamalad7.mixinextras.expression.impl.serialization.SerializedExpressionId;
import com.llamalad7.mixinextras.utils.Decorations;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.IOException;

@SerializedExpressionId("[]")
public class ArrayAccessExpression implements SimpleExpression {
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

    @Override
    public void capture(FlowValue node, OutputSink sink) {
        sink.decorate(node.getInsn(), Decorations.SIMPLE_OPERATION_ARGS, new Type[]{node.getInput(0).getType(), Type.INT_TYPE});
        sink.decorate(node.getInsn(), Decorations.SIMPLE_OPERATION_RETURN_TYPE, node.getType());
        SimpleExpression.super.capture(node, sink);
    }

    @Override
    public void write(ExpressionWriter writer) throws IOException {
        writer.writeExpression(arr);
        writer.writeExpression(index);
    }

    public static Expression read(ExpressionReader reader) throws IOException {
        return new ArrayAccessExpression(reader.readExpression(), reader.readExpression());
    }
}
