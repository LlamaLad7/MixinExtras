package com.llamalad7.mixinextras.expression.impl.flow.expansion;

import com.llamalad7.mixinextras.expression.impl.flow.FlowPostProcessor;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;

import java.util.Arrays;
import java.util.List;

public class ExpansionPostProcessor implements FlowPostProcessor {
    private final List<InsnExpander> expanders = Arrays.asList(
            new IincExpander(), new UnaryComparisonExpander()
    );

    @Override
    public void process(FlowValue node, OutputSink sink) {
        for (InsnExpander expander : expanders) {
            expander.expand(node, sink);
        }
    }
}
