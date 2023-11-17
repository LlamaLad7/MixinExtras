package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool;
import com.llamalad7.mixinextras.expression.impl.serialization.SerializedTypeName;
import org.objectweb.asm.Opcodes;

@SerializedTypeName("null")
public class NullLiteralExpression implements SimpleExpression {
    @Override
    public boolean matches(FlowValue node, IdentifierPool pool, OutputSink sink) {
        return node.getInsn().getOpcode() == Opcodes.ACONST_NULL;
    }
}
