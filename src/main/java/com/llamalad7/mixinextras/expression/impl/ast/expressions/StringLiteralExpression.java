package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool;
import org.spongepowered.asm.util.Bytecode;

import java.util.Objects;

public class StringLiteralExpression implements SimpleExpression {
    private static final long serialVersionUID = -3297972448725290268L;

    public final String value;

    public StringLiteralExpression(String value) {
        this.value = value;
    }

    @Override
    public boolean matches(FlowValue node, IdentifierPool pool, OutputSink sink) {
        Object cst = Bytecode.getConstant(node.getInsn());
        return Objects.equals(cst, value) || (!value.isEmpty() && Objects.equals(cst, value.charAt(0)));
    }
}
