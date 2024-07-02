package com.llamalad7.mixinextras.expression.impl.flow.postprocessing;

import com.llamalad7.mixinextras.expression.impl.flow.DummyFlowValue;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;

public class SplitNodeRemovalPostProcessor implements FlowPostProcessor {
    @Override
    public void process(FlowValue node, OutputSink sink) {
        if (node.getNext().size() <= 1) {
            // The node is not split.
            return;
        }
        FlowValue successor = new DummyFlowValue(node.getType());
        for (Pair<FlowValue, Integer> next : new ArrayList<>(node.getNext())) {
            FlowValue target = next.getLeft();
            int index = next.getRight();
            target.setParent(index, successor);
        }
    }
}
