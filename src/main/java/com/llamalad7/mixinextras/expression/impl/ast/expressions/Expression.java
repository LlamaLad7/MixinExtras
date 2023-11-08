package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool;
import com.llamalad7.mixinextras.utils.Decorations;
import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;

public interface Expression {
    default boolean matches(FlowValue node, IdentifierPool pool, CaptureSink sink) {
        return false;
    }

    default void capture(FlowValue node, CaptureSink sink) {
        if (node.getType() == Type.VOID_TYPE) {
            sink.accept(node.getInsn());
            return;
        }
        sink.acceptExpression(node);
    }

    default boolean inputsMatch(FlowValue node, IdentifierPool pool, CaptureSink sink, Expression... values) {
        return inputsMatch(0, node, pool, sink, values);
    }

    default boolean inputsMatch(int start, FlowValue node, IdentifierPool pool, CaptureSink sink, Expression... values) {
        for (int i = 0; i < values.length; i++) {
            Expression value = values[i];
            if (!value.matches(node.getInput(i + start), pool, sink)) {
                return false;
            }
        }
        return true;
    }

    interface CaptureSink {
        void accept(AbstractInsnNode node, Pair<String, Object>... decorations);

        default void acceptExpression(FlowValue node) {
            accept(node.getInsn(), Pair.of(Decorations.SIMPLE_EXPRESSION_TYPE, node.getType()));
        }
    }
}
