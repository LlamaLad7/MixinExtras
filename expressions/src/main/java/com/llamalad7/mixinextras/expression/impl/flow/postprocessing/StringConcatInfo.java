package com.llamalad7.mixinextras.expression.impl.flow.postprocessing;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;

public class StringConcatInfo {
    public final boolean isFirstConcat;
    public final FlowValue initialComponent;
    public final FlowValue toStringCall;

    public StringConcatInfo(boolean isFirstConcat, FlowValue initialComponent, FlowValue toStringCall) {
        this.isFirstConcat = isFirstConcat;
        this.initialComponent = initialComponent;
        this.toStringCall = toStringCall;
    }
}
