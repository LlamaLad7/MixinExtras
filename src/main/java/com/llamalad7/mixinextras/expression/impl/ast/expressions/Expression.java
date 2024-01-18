package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;
import org.objectweb.asm.tree.AbstractInsnNode;

public interface Expression {
    default boolean matches(FlowValue node, ExpressionContext ctx) {
        return false;
    }

    default void capture(FlowValue node, OutputSink sink) {
        sink.capture(node.getInsn());
    }

    default boolean inputsMatch(FlowValue node, ExpressionContext ctx, Expression... values) {
        return inputsMatch(0, node, ctx, values);
    }

    default boolean inputsMatch(int start, FlowValue node, ExpressionContext ctx, Expression... values) {
        if (values.length != node.inputCount() - start) {
            return false;
        }
        for (int i = 0; i < values.length; i++) {
            Expression value = values[i];
            if (!value.matches(node.getInput(i + start), ctx)) {
                return false;
            }
        }
        return true;
    }

    interface OutputSink {
        void capture(AbstractInsnNode insn);

        void decorate(AbstractInsnNode insn, String key, Object value);

        void decorateInjectorSpecific(AbstractInsnNode insn, String key, Object value);
    }
}
