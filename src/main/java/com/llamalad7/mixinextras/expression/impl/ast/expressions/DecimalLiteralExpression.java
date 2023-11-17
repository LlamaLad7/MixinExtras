package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool;
import org.spongepowered.asm.util.Bytecode;

public class DecimalLiteralExpression implements SimpleExpression {
    private static final long serialVersionUID = -4921919181011862271L;

    public final double value;

    public DecimalLiteralExpression(double value) {
        this.value = value;
    }

    @Override
    public boolean matches(FlowValue node, IdentifierPool pool, OutputSink sink) {
        Object cst = Bytecode.getConstant(node.getInsn());
        if (cst == null) {
            return false;
        }
        return (cst instanceof Float || cst instanceof Double) && ((Number) cst).doubleValue() == value;
    }
}
