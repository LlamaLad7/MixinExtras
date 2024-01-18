package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool;
import org.objectweb.asm.Opcodes;

public class NullLiteralExpression implements SimpleExpression {
    @Override
    public boolean matches(FlowValue node, IdentifierPool pool, OutputSink sink) {
        return node.getInsn().getOpcode() == Opcodes.ACONST_NULL;
    }
}
