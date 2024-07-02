package com.llamalad7.mixinextras.expression.impl.point;

import com.llamalad7.mixinextras.expression.impl.MEExpressionService;
import com.llamalad7.mixinextras.utils.CompatibilityHelper;
import com.llamalad7.mixinextras.utils.InjectorUtils;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes;

class RuntimeExpressionService extends MEExpressionService {
    @Override
    public RuntimeException makeInvalidInjectionException(InjectionInfo info, String message) {
        return CompatibilityHelper.makeInvalidInjectionException(info, message);
    }

    @Override
    public void decorateInjectorSpecific(InjectionNodes.InjectionNode node, InjectionInfo info, String key, Object value) {
        InjectorUtils.decorateInjectorSpecific(node, info, key, value);
    }
}
