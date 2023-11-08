package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.google.gson.annotations.SerializedName;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool;
import com.llamalad7.mixinextras.expression.impl.serialization.SerializedTypeName;
import org.spongepowered.asm.util.Bytecode;

import java.util.Objects;

@SerializedTypeName("str")
public class StringLiteralExpression implements Expression {
    @SerializedName("v")
    public final String value;

    public StringLiteralExpression(String value) {
        this.value = value;
    }

    @Override
    public boolean matches(FlowValue node, IdentifierPool pool, CaptureSink sink) {
        Object cst = Bytecode.getConstant(node.getInsn());
        return Objects.equals(cst, value) || (!value.isEmpty() && Objects.equals(cst, value.charAt(0)));
    }
}
