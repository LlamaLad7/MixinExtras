package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.google.gson.annotations.SerializedName;
import com.llamalad7.mixinextras.expression.impl.ast.identifiers.Identifier;
import com.llamalad7.mixinextras.expression.impl.serialization.SerializedTypeName;

@SerializedTypeName("=")
public class IdentifierAssignmentExpression implements Expression {
    @SerializedName("id")
    public final Identifier identifier;
    @SerializedName("v")
    public final Expression value;

    public IdentifierAssignmentExpression(Identifier identifier, Expression value) {
        this.identifier = identifier;
        this.value = value;
    }
}
