package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.google.gson.annotations.SerializedName;
import com.llamalad7.mixinextras.expression.impl.serialization.SerializedTypeName;

@SerializedTypeName("cmp")
public class ComparisonExpression implements Expression {
    @SerializedName("l")
    public final Expression left;
    @SerializedName("op")
    public final Operator operator;
    @SerializedName("r")
    public final Expression right;

    public ComparisonExpression(Expression left, Operator operator, Expression right) {
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

    public enum Operator {
        @SerializedName("==")
        EQ,
        @SerializedName("!=")
        NE,
        @SerializedName("<")
        LT,
        @SerializedName("<=")
        LE,
        @SerializedName(">")
        GT,
        @SerializedName(">=")
        GE
    }
}
