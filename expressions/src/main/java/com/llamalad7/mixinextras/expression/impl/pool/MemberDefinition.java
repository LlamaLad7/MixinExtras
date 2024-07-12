package com.llamalad7.mixinextras.expression.impl.pool;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;

public interface MemberDefinition {
    boolean matches(FlowValue node);
}
