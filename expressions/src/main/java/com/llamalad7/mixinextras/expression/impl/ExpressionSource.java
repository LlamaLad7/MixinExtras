package com.llamalad7.mixinextras.expression.impl;

public class ExpressionSource {
    public final String expression;
    public final int startIndex;
    public final int endIndex;

    public ExpressionSource(String expression, int startIndex, int endIndex) {
        this.expression = expression;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
    }

    @Override
    public String toString() {
        return expression.substring(startIndex, endIndex + 1);
    }
}
