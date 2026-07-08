package com.llamalad7.mixinextras.expression.impl.ast;

import com.llamalad7.mixinextras.expression.impl.ast.expressions.Expression;

public interface Argument {
    Argument ELLIPSIS = new Argument() {
    };

    class Concrete implements Argument {
        public final Expression expression;

        public Concrete(Expression expression) {
            this.expression = expression;
        }
    }
}
