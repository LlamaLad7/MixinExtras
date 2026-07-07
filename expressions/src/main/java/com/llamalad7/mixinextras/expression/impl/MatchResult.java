package com.llamalad7.mixinextras.expression.impl;

import com.llamalad7.mixinextras.expression.impl.ast.expressions.Expression;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import org.objectweb.asm.tree.AbstractInsnNode;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class MatchResult {
    public static final MatchResult SUCCESS = new MatchResult(sink -> {});
    public static final MatchResult FAILURE = new MatchResult(null);

    private final Consumer<Expression.OutputSink> action;

    private MatchResult(Consumer<Expression.OutputSink> action) {
        this.action = action;
    }

    public boolean isSuccess() {
        return action != null;
    }

    public void perform(Expression.OutputSink sink) {
        action.accept(sink);
    }

    public MatchResult then(MatchResult other) {
        if (!isSuccess() || !other.isSuccess()) {
            return FAILURE;
        }
        return new MatchResult(action.andThen(other.action));
    }

    public MatchResult then(Supplier<MatchResult> other) {
        if (!isSuccess()) {
            return FAILURE;
        }
        return then(other.get());
    }

    public MatchResult thenCapture(FlowValue node, Expression expression) {
        return then(new MatchResult(sink -> sink.capture(node, expression)));
    }

    public MatchResult thenDecorate(AbstractInsnNode insn, String key, Object value) {
        return then(new MatchResult(sink -> sink.decorate(insn, key, value)));
    }

    public MatchResult thenDecorateInjectorSpecific(AbstractInsnNode insn, String key, Object value) {
        return then(new MatchResult(sink -> sink.decorateInjectorSpecific(insn, key, value)));
    }

    public static MatchResult basic(boolean success) {
        return success ? SUCCESS : FAILURE;
    }
}
