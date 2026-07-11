package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ExpressionSource;
import com.llamalad7.mixinextras.expression.impl.MatchResult;
import com.llamalad7.mixinextras.expression.impl.ast.Argument;
import com.llamalad7.mixinextras.expression.impl.flow.ComplexDataException;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;
import com.llamalad7.mixinextras.expression.impl.utils.GlobMatching;
import org.objectweb.asm.tree.AbstractInsnNode;

import java.util.ArrayList;
import java.util.List;

public abstract class Expression {
    protected final ExpressionSource src;

    public Expression(ExpressionSource src) {
        this.src = src;
    }

    public ExpressionSource getSrc() {
        return src;
    }

    public final MatchResult match(FlowValue node, ExpressionContext ctx) {
        ctx.reportMatchAttempt(node, this);
        MatchResult result;
        try {
            result = matchImpl(node, ctx);
        } catch (ComplexDataException ignored) {
            result = MatchResult.FAILURE;
        }
        ctx.reportMatchStatus(node, this, result.isSuccess());
        return result;
    }

    protected abstract MatchResult matchImpl(FlowValue node, ExpressionContext ctx);

    protected MatchResult capture(FlowValue node, ExpressionContext ctx, MatchResult result) {
        return result.thenCapture(node, this);
    }

    protected MatchResult matchInputs(FlowValue node, ExpressionContext ctx, Expression... values) {
        return matchInputs(node, ctx, false, values);
    }

    protected MatchResult matchInputs(FlowValue node, ExpressionContext ctx, boolean allowIncomplete, Expression... values) {
        return matchInputs(0, node, ctx, allowIncomplete, values);
    }

    protected MatchResult matchInputs(int start, FlowValue node, ExpressionContext ctx, Expression... values) {
        return matchInputs(start, node, ctx, false, values);
    }

    protected MatchResult matchInputs(int start, FlowValue node, ExpressionContext ctx, boolean allowIncomplete, Expression... values) {
        // If we're checking inputs, then we must have matched partially
        ctx.reportPartialMatch(node, this);

        int required = node.inputCount();
        if (!(allowIncomplete && values.length < required) && values.length != required) {
            return null;
        }
        MatchResult result = MatchResult.SUCCESS;
        for (int i = 0; i < values.length; i++) {
            Expression value = values[i];
            MatchResult innerResult = value.match(node.getInput(i + start), ctx);
            if (!innerResult.isSuccess()) {
                return MatchResult.FAILURE;
            }
            result = result.then(innerResult);
        }
        return result;
    }

    protected MatchResult matchArguments(FlowValue node, ExpressionContext ctx, List<Argument> arguments) {
        return matchArguments(0, node, ctx, arguments);
    }

    protected MatchResult matchArguments(int start, FlowValue node, ExpressionContext ctx, List<Argument> arguments) {
        // If we're checking arguments, then we must have matched partially
        ctx.reportPartialMatch(node, this);

        if (ctx.allowIncompleteListInputs) {
            arguments = new ArrayList<>(arguments);
            arguments.add(Argument.ELLIPSIS);
        }
        return GlobMatching.match(ctx, node, arguments, start);
    }

    public interface OutputSink {
        void capture(FlowValue node, Expression expr);

        void decorate(AbstractInsnNode insn, String key, Object value);

        void decorateInjectorSpecific(AbstractInsnNode insn, String key, Object value);

        default void reportMatchAttempt(FlowValue node, Expression expr) {
        }

        default void reportMatchStatus(FlowValue node, Expression expr, boolean matched) {
        }

        default void reportPartialMatch(FlowValue node, Expression expr) {
        }

        default void reportArgumentMatchingFork(FlowValue node, int preserveInputs) {
        }
    }
}
