package com.llamalad7.mixinextras.expression.impl.flow.postprocessing;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.flow.utils.InsnReference;

public class ArrayCreationInfo {
    public final InsnReference initialized;

    public ArrayCreationInfo(FlowValue initialized) {
        this.initialized = new InsnReference(initialized);
    }
}
