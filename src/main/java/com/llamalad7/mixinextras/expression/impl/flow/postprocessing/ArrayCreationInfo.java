package com.llamalad7.mixinextras.expression.impl.flow.postprocessing;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import org.objectweb.asm.tree.AbstractInsnNode;

import java.util.List;

public class ArrayCreationInfo {
    public final List<FlowValue> values;
    public final AbstractInsnNode initialized;

    public ArrayCreationInfo(List<FlowValue> values, AbstractInsnNode initialized) {
        this.values = values;
        this.initialized = initialized;
    }
}
