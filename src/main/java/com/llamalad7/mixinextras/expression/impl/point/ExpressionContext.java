package com.llamalad7.mixinextras.expression.impl.point;

import com.llamalad7.mixinextras.expression.impl.ast.expressions.Expression;
import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool;
import org.spongepowered.asm.mixin.injection.struct.Target;

public class ExpressionContext {
    private final IdentifierPool pool;
    private final Expression.OutputSink sink;
    private final Target target;

    public ExpressionContext(IdentifierPool pool, Expression.OutputSink sink, Target target) {
        this.pool = pool;
        this.sink = sink;
        this.target = target;
    }

    public IdentifierPool getPool() {
        return pool;
    }

    public Expression.OutputSink getSink() {
        return sink;
    }

    public Target getTarget() {
        return target;
    }
}
