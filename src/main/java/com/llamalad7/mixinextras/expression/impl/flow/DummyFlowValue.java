package com.llamalad7.mixinextras.expression.impl.flow;

import com.llamalad7.mixinextras.utils.ASMUtils;
import org.objectweb.asm.Type;

public class DummyFlowValue extends FlowValue {
    public static final FlowValue UNINITIALIZED = new DummyFlowValue(Type.VOID_TYPE);

    public DummyFlowValue(Type type) {
        super(type.getSize(), inputs -> type, null);
    }

    @Override
    public FlowValue mergeWith(FlowValue other) {
        DummyFlowValue otherDummy = (DummyFlowValue) other;
        if (otherDummy.getType().equals(getType())) {
            return this;
        }
        return new DummyFlowValue(ASMUtils.getCommonSupertype(getType(), otherDummy.getType()));
    }
}
