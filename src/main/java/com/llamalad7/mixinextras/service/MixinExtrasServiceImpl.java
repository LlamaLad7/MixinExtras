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
import org.apache.commons.lang3.StringUtils;
import org.objectweb.asm.Type;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.transformer.ext.IExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MixinExtrasServiceImpl implements MixinExtrasService {
    private static final MixinExtrasLogger LOGGER = MixinExtrasLogger.get("MixinExtras|Service");

    private final List<Versioned<String>> offeredPackages = new ArrayList<>();
    private final List<Versioned<IExtension>> offeredExtensions = new ArrayList<>();
    private final List<Versioned<Class<? extends InjectionInfo>>> offeredInjectors = new ArrayList<>();
    private final String ownPackage = StringUtils.substringBefore(getClass().getName(), ".service.");
    private final List<IExtension> ownExtensions = Arrays.asList(
            new ServiceInitializationExtension(this), new SugarApplicatorExtension(), new WrapOperationApplicatorExtension(),
            new SugarPostProcessingExtension()
    );
    private final List<Class<? extends InjectionInfo>> ownInjectors = Arrays.asList(
            ModifyExpressionValueInjectionInfo.class, ModifyReceiverInjectionInfo.class, ModifyReturnValueInjectionInfo.class,
            WrapOperationInjectionInfo.class, WrapWithConditionInjectionInfo.class, SugarWrapperInjectionInfo.class
    );

    boolean initialized;
    private List<String> allPackages;

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
        requireNotInitialized();
        LOGGER.debug("{} is conceding to {}", this, newerService);
        MixinExtrasService newService = MixinExtrasService.getFrom(newerService);
        for (Versioned<String> packageName : offeredPackages) {
            newService.offerPackage(packageName.version, packageName.value);
        }
        newService.offerPackage(getVersion(), ownPackage);
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
    public void offerPackage(int version, String packageName) {
        requireNotInitialized();
        offeredPackages.add(new Versioned<>(version, packageName));
    }

    @Override
    public void offerExtension(int version, IExtension extension) {
        requireNotInitialized();
        offeredExtensions.add(new Versioned<>(version, extension));
    }

    @Override
    public void offerInjector(int version, Class<? extends InjectionInfo> injector) {
        requireNotInitialized();
        offeredInjectors.add(new Versioned<>(version, injector));
    }

    @Override
    public String toString() {
        return String.format(
                "%s(version=%s)",
                getClass().getName(), MixinExtrasVersion.LATEST
        );
    }

    @Override
    public void initialize() {
        requireNotInitialized();
        LOGGER.info("Initializing MixinExtras version {} via {}.", MixinExtrasBootstrap.getVersion(), this);
        allPackages = new ArrayList<>();
        allPackages.add(ownPackage);
        for (Versioned<String> otherPackage : offeredPackages) {
            allPackages.add(otherPackage.value);
        }
        ownInjectors.forEach(this::registerInjector);
        initialized = true;
    }

    private void registerInjector(Class<? extends InjectionInfo> injector) {
        String name = injector.getAnnotation(InjectionInfo.AnnotationType.class).value().getName();
        String suffix = StringUtils.removeStart(name, ownPackage);
        for (String packageName : allPackages) {
            MixinInternals.registerInjector(packageName + suffix, injector);
        }
    }

    public Type changePackage(Class<?> ourType, Type theirReference, Class<?> ourReference) {
        String suffix = StringUtils.substringAfter(ourReference.getName(), ownPackage);
        String theirPackage = StringUtils.substringBefore(theirReference.getClassName(), suffix);
        return Type.getObjectType((theirPackage + StringUtils.substringAfter(ourType.getName(), ownPackage)).replace('.', '/'));
    }

    public Set<String> getAllClassNames(String ourName) {
        String ourBinaryName = ourName.replace('/', '.');
        return allPackages.stream().map(it -> StringUtils.replaceOnce(ourBinaryName, ownPackage, it)).collect(Collectors.toSet());
    }

    public boolean isClassOwned(String name) {
        return allPackages.stream().anyMatch(name::startsWith);
    }

    private void requireNotInitialized() {
        if (initialized) {
            throw new IllegalStateException("The MixinExtras service has already been selected and is initialized!");
        }
    }
}
