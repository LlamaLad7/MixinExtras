package com.llamalad7.mixinextras.service;

import com.llamalad7.mixinextras.utils.Blackboard;
import com.llamalad7.mixinextras.utils.ProxyUtils;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.transformer.ext.IExtension;

public interface MixinExtrasService {
    int getVersion();

    boolean shouldReplace(Object otherService);

    void takeControlFrom(Object olderService);

    void concedeTo(Object newerService, boolean wasActive);

    void offerPackage(int version, String packageName);

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
        return ProxyUtils.getProxy(serviceImpl, MixinExtrasService.class);
    }

    static MixinExtrasServiceImpl getInstance() {
        Object impl = Blackboard.get("MixinExtrasServiceInstance");
        if (impl instanceof MixinExtrasServiceImpl) {
            MixinExtrasServiceImpl ourImpl = (MixinExtrasServiceImpl) impl;
            if (ourImpl.initialized) {
                return ourImpl;
            }
            throw new IllegalStateException("Cannot use service because it is not initialized!");
        }
        throw new IllegalStateException("Cannot use service because another service is active: " + impl);
    }
}
