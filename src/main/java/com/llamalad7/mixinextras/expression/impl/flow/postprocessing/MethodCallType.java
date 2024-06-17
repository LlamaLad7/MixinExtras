package com.llamalad7.mixinextras.expression.impl.flow.postprocessing;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.utils.Decorations;

public enum MethodCallType {
    NORMAL,
    SUPER,
    STATIC;

    public boolean matches(FlowValue node) {
        return node.getDecoration(Decorations.METHOD_CALL_TYPE) == this;
    }
}
