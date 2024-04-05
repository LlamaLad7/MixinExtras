package com.llamalad7.mixinextras.expression.impl.flow;

public interface FlowPostProcessor {
    void process(FlowValue node, OutputSink sink);

    interface OutputSink {
        void markAsSynthetic(FlowValue node);

        void registerFlow(FlowValue... nodes);
    }
}
