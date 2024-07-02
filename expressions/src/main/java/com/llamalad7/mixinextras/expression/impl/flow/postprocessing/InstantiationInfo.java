package com.llamalad7.mixinextras.expression.impl.flow.postprocessing;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import org.objectweb.asm.Type;

public class InstantiationInfo {
    public final Type type;
    public final FlowValue initCall;

    public InstantiationInfo(Type type, FlowValue initCall) {
        this.type = type;
        this.initCall = initCall;
    }
}
