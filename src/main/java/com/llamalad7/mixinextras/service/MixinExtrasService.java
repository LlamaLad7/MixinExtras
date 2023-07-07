package com.llamalad7.mixinextras.service;

import com.llamalad7.mixinextras.utils.Blackboard;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.transformer.ext.IExtension;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

public interface MixinExtrasService {
    int getVersion();

    boolean shouldReplace(Object otherService);

    void takeControlFrom(Object olderService);

    void concedeTo(Object newerService, boolean wasActive);

    void offerExtension(int version, IExtension extension);

    void offerInjector(int version, Class<? extends InjectionInfo> injector);

    void initialize();

    static void setup() {
        Object latestImpl = Blackboard.get("MixinExtrasServiceInstance");
        if (latestImpl == null) {
            MixinExtrasService newImpl = new MixinExtrasServiceImpl();
            Blackboard.put("MixinExtrasServiceInstance", newImpl);
            newImpl.takeControlFrom(null);
            return;
        }
        MixinExtrasService ourImpl = new MixinExtrasServiceImpl();
        if (ourImpl.shouldReplace(latestImpl)) {
            getFrom(latestImpl).concedeTo(ourImpl, true);
            Blackboard.put("MixinExtrasServiceInstance", ourImpl);
            ourImpl.takeControlFrom(latestImpl);
        } else {
            ourImpl.concedeTo(latestImpl, false);
        }
    }

    static MixinExtrasService getFrom(Object serviceImpl) {
        if (Arrays.stream(serviceImpl.getClass().getInterfaces()).anyMatch(it -> it.getName().endsWith(".MixinExtrasService"))) {
            return (MixinExtrasService) Proxy.newProxyInstance(MixinExtrasService.class.getClassLoader(), new Class[]{MixinExtrasService.class}, (proxy, method, args) -> {
                Method original = serviceImpl.getClass().getMethod(method.getName(), method.getParameterTypes());
                original.setAccessible(true);
                return original.invoke(serviceImpl, args);
            });
        }
        throw new UnsupportedOperationException("Cannot get a MixinExtrasService from " + serviceImpl);
    }
}
