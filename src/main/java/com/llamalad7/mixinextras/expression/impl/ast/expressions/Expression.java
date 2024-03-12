package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ExpressionSource;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;
import org.objectweb.asm.tree.AbstractInsnNode;

public abstract class Expression {
    protected final ExpressionSource src;

    public Expression(ExpressionSource src) {
        this.src = src;
    }

    public ExpressionSource getSrc() {
        return src;
    }

    public boolean matches(FlowValue node, ExpressionContext ctx) {
        return false;
    }

    public void capture(FlowValue node, OutputSink sink) {
        sink.capture(node, this);
    }

    public boolean inputsMatch(FlowValue node, ExpressionContext ctx, Expression... values) {
        return inputsMatch(node, ctx, false, values);
    }

    public boolean inputsMatch(FlowValue node, ExpressionContext ctx, boolean allowIncomplete, Expression... values) {
        return inputsMatch(0, node, ctx, allowIncomplete, values);
    }

    public boolean inputsMatch(int start, FlowValue node, ExpressionContext ctx, Expression... values) {
        return inputsMatch(start, node, ctx, false, values);
    }

    public boolean inputsMatch(int start, FlowValue node, ExpressionContext ctx, boolean allowIncomplete, Expression... values) {
        int required = node.inputCount() - start;
        if (!(allowIncomplete && values.length < required) && values.length != required) {
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

    public interface OutputSink {
        void capture(FlowValue node, Expression expr);

        void decorate(AbstractInsnNode insn, String key, Object value);

        void decorateInjectorSpecific(AbstractInsnNode insn, String key, Object value);
    }
}
