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

    public final boolean matches(FlowValue node, ExpressionContext ctx) {
        boolean result = matchesImpl(node, ctx);
        ctx.reportMatchStatus(node, this, result);
        return result;
    }

    protected boolean matchesImpl(FlowValue node, ExpressionContext ctx) {
        return false;
    }

    protected void capture(FlowValue node, ExpressionContext ctx) {
        ctx.capture(node, this);
    }

    protected boolean inputsMatch(FlowValue node, ExpressionContext ctx, Expression... values) {
        return inputsMatch(node, ctx, false, values);
    }

    protected boolean inputsMatch(FlowValue node, ExpressionContext ctx, boolean allowIncomplete, Expression... values) {
        return inputsMatch(0, node, ctx, allowIncomplete, values);
    }

    protected boolean inputsMatch(int start, FlowValue node, ExpressionContext ctx, Expression... values) {
        return inputsMatch(start, node, ctx, false, values);
    }

    protected boolean inputsMatch(int start, FlowValue node, ExpressionContext ctx, boolean allowIncomplete, Expression... values) {
        // If we're checking inputs, then we must have matched partially
        ctx.reportPartialMatch(node, this);

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
        void capture(FlowValue node, Expression expr, ExpressionContext ctx);

        void decorate(AbstractInsnNode insn, String key, Object value);

        void decorateInjectorSpecific(AbstractInsnNode insn, String key, Object value);

        default void reportMatchStatus(FlowValue node, Expression expr, boolean matched) {
        }

        default void reportPartialMatch(FlowValue node, Expression expr) {
        }
    }
}
