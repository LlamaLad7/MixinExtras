package com.llamalad7.mixinextras.injector;

import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.service.MixinExtrasService;
import com.llamalad7.mixinextras.sugar.impl.SugarWrapperInjectionInfo;
import com.llamalad7.mixinextras.utils.MixinExtrasLogger;
import com.llamalad7.mixinextras.utils.ProxyUtils;
import org.apache.commons.lang3.ClassUtils;

import java.lang.reflect.InvocationTargetException;

public interface LateApplyingInjectorInfo {
    void lateInject();

    void latePostInject();

    @SuppressWarnings("unused")
    void wrap(LateApplyingInjectorInfo outer);

    String getLateInjectionType();

    /**
     * Will be called by 0.2.0-beta.1 when enqueueing a {@link SugarWrapperInjectionInfo} for a {@link WrapOperation}.
     */
    @Deprecated
    default void lateApply() {
        lateInject();
        MixinExtrasLogger logger = MixinExtrasLogger.get("Sugar");
        logger.warn("Skipping post injection checks for {} since it is from 0.2.0-beta.1 and cannot be saved", this);
    }

    /**
     * Handles the inner injection info being from a different package to ours.
     */
    static boolean wrap(Object inner, LateApplyingInjectorInfo outer) {
        Class<?> theirInterface = ClassUtils.getAllInterfaces(inner.getClass()).stream()
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
