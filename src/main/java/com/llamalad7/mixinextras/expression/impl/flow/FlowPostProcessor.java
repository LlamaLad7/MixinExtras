package com.llamalad7.mixinextras.expression.impl.flow;

import java.util.function.Consumer;

public interface FlowPostProcessor {
    void process(FlowValue node, Consumer<FlowValue> syntheticMarker);
}
