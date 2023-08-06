package com.llamalad7.mixinextras.injector;

import com.llamalad7.mixinextras.service.MixinExtrasService;
import com.llamalad7.mixinextras.utils.ProxyUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

public interface LateApplyingInjectorInfo {
    void lateInject();

    void latePostInject();

    void wrap(LateApplyingInjectorInfo outer);

    /**
     * Handles the inner injection info being from a different package to ours.
     */
    static boolean wrap(Object inner, LateApplyingInjectorInfo outer) {
        Class<?> theirInterface = Arrays.stream(inner.getClass().getInterfaces())
                .filter(it -> it.getName().endsWith(".LateApplyingInjectorInfo")).findFirst().orElse(null);
        if (theirInterface == null || !MixinExtrasService.getInstance().isClassOwned(theirInterface.getName())) {
            return false;
        }
        try {
            inner.getClass().getMethod("wrap", theirInterface).invoke(inner, ProxyUtils.getProxy(outer, theirInterface));
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException("Failed to wrap InjectionInfo: ", e);
        }
        return true;
    }
}
