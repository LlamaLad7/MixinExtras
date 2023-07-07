package com.llamalad7.mixinextras.service;

import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import com.llamalad7.mixinextras.injector.ModifyExpressionValueInjectionInfo;
import com.llamalad7.mixinextras.injector.ModifyReceiverInjectionInfo;
import com.llamalad7.mixinextras.injector.ModifyReturnValueInjectionInfo;
import com.llamalad7.mixinextras.injector.WrapWithConditionInjectionInfo;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperationApplicatorExtension;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperationInjectionInfo;
import com.llamalad7.mixinextras.sugar.impl.SugarApplicatorExtension;
import com.llamalad7.mixinextras.sugar.impl.SugarPostProcessingExtension;
import com.llamalad7.mixinextras.sugar.impl.SugarWrapperInjectionInfo;
import com.llamalad7.mixinextras.utils.MixinExtrasLogger;
import com.llamalad7.mixinextras.utils.MixinInternals;
import com.llamalad7.mixinextras.utils.UniquenessHelper;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.transformer.ext.IExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class MixinExtrasServiceImpl implements MixinExtrasService {
    private static final MixinExtrasLogger LOGGER = MixinExtrasLogger.get("MixinExtras|Service");

    private final List<Versioned<IExtension>> offeredExtensions = new ArrayList<>();

    private final List<Versioned<Class<? extends InjectionInfo>>> offeredInjectors = new ArrayList<>();

    private final List<IExtension> ownExtensions = Arrays.asList(
            new ServiceInitializationExtension(this), new SugarApplicatorExtension(), new WrapOperationApplicatorExtension(),
            new SugarPostProcessingExtension(), new UniquenessHelper.Extension()
    );

    private final List<Class<? extends InjectionInfo>> ownInjectors = Arrays.asList(
            ModifyExpressionValueInjectionInfo.class, ModifyReceiverInjectionInfo.class, ModifyReturnValueInjectionInfo.class,
            WrapOperationInjectionInfo.class, WrapWithConditionInjectionInfo.class, SugarWrapperInjectionInfo.class
    );

    @Override
    public int getVersion() {
        return MixinExtrasVersion.LATEST.ordinal();
    }

    @Override
    public boolean shouldReplace(Object otherService) {
        return getVersion() > MixinExtrasService.getFrom(otherService).getVersion();
    }

    @Override
    public void takeControlFrom(Object olderService) {
        LOGGER.debug("{} is taking over from {}", this, olderService);
        ownExtensions.forEach(MixinInternals::registerExtension);
    }

    @Override
    public void concedeTo(Object newerService, boolean wasActive) {
        LOGGER.debug("{} is conceding to {}", this, newerService);
        MixinExtrasService newService = MixinExtrasService.getFrom(newerService);
        for (Versioned<IExtension> extension : offeredExtensions) {
            newService.offerExtension(extension.version, extension.value);
        }
        for (IExtension extension : ownExtensions) {
            if (wasActive) {
                MixinInternals.unregisterExtension(extension);
            }
            newService.offerExtension(getVersion(), extension);
        }
        for (Versioned<Class<? extends InjectionInfo>> injector : offeredInjectors) {
            newService.offerInjector(injector.version, injector.value);
        }
        for (Class<? extends InjectionInfo> injector : ownInjectors) {
            newService.offerInjector(getVersion(), injector);
        }
    }

    @Override
    public void offerExtension(int version, IExtension extension) {
        offeredExtensions.add(new Versioned<>(version, extension));
    }

    @Override
    public void offerInjector(int version, Class<? extends InjectionInfo> injector) {
        offeredInjectors.add(new Versioned<>(version, injector));
    }

    @Override
    public void initialize() {
        LOGGER.info("Initializing MixinExtras version {} via {}.", MixinExtrasBootstrap.getVersion(), this);
        ownInjectors.forEach(injector -> {
            MixinInternals.registerInjector(injector.getAnnotation(InjectionInfo.AnnotationType.class).value(), injector);
        });
    }

    @Override
    public String toString() {
        return String.format(
                "%s(version=%s)",
                getClass().getName(), MixinExtrasVersion.LATEST
        );
    }
}
