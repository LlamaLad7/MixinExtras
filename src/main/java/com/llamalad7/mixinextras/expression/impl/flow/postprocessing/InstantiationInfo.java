package com.llamalad7.mixinextras.expression.impl.flow.postprocessing;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import org.objectweb.asm.Type;

import java.util.List;

public class InstantiationInfo {
    public final Type type;
    public final FlowValue initCall;
    public final List<FlowValue> args;

    public InstantiationInfo(Type type, FlowValue initCall, List<FlowValue> args) {
        this.type = type;
        this.initCall = initCall;
        this.args = args;
    }
}
