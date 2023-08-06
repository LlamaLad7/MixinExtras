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

import java.util.*;
import java.util.stream.Collectors;

public class MixinExtrasServiceImpl implements MixinExtrasService {
    private static final MixinExtrasLogger LOGGER = MixinExtrasLogger.get("MixinExtras|Service");

    private final List<Versioned<String>> offeredPackages = new ArrayList<>();
    private final List<Versioned<IExtension>> offeredExtensions = new ArrayList<>();
    private final List<Versioned<Class<? extends InjectionInfo>>> offeredInjectors = new ArrayList<>();
    private final String ownPackage = StringUtils.substringBefore(getClass().getName(), ".service.");
    private final List<String> allPackages = new ArrayList<>(Collections.singletonList(ownPackage));
    private final List<IExtension> ownExtensions = Arrays.asList(
            new SugarApplicatorExtension(), new ServiceInitializationExtension(this),
            new WrapOperationApplicatorExtension(), new SugarPostProcessingExtension()
    );
    private final List<Class<? extends InjectionInfo>> ownInjectors = Arrays.asList(
            ModifyExpressionValueInjectionInfo.class, ModifyReceiverInjectionInfo.class, ModifyReturnValueInjectionInfo.class,
            WrapOperationInjectionInfo.class, WrapWithConditionInjectionInfo.class, SugarWrapperInjectionInfo.class
    );

    boolean initialized;

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
        ownExtensions.forEach(it -> {
            // Hack to "support" old betas.
            // Our new applicator *must* be present in the list before any old ones, which this ensures.
            // We can then hide the sugar from them, so they remain inactive.
            // We prioritise the initialization extension so it's definitely before the sugar one.
            MixinInternals.registerExtension(it, it instanceof ServiceInitializationExtension || it instanceof SugarApplicatorExtension);
        });
        ownInjectors.forEach(it -> registerInjector(it, ownPackage));
    }

    @Override
    public void concedeTo(Object newerService, boolean wasActive) {
        requireNotInitialized();
        LOGGER.debug("{} is conceding to {}", this, newerService);
        MixinExtrasService newService = MixinExtrasService.getFrom(newerService);

        if (wasActive) {
            deInitialize();
        }

        offeredPackages.forEach(packageName -> newService.offerPackage(packageName.version, packageName.value));
        newService.offerPackage(getVersion(), ownPackage);

        offeredExtensions.forEach(extension -> newService.offerExtension(extension.version, extension.value));
        ownExtensions.forEach(extension -> newService.offerExtension(getVersion(), extension));

        offeredInjectors.forEach(injector -> newService.offerInjector(injector.version, injector.value));
        ownInjectors.forEach(injector -> newService.offerInjector(getVersion(), injector));
    }

    @Override
    public void offerPackage(int version, String packageName) {
        requireNotInitialized();
        offeredPackages.add(new Versioned<>(version, packageName));
        allPackages.add(packageName);
        ownInjectors.forEach(it -> registerInjector(it, packageName));
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
        detectBetaPackages();
        initialized = true;
    }

    private void deInitialize() {
        for (IExtension extension : ownExtensions) {
            MixinInternals.unregisterExtension(extension);
        }
        for (Class<? extends InjectionInfo> injector : ownInjectors) {
            allPackages.forEach(it -> unregisterInjector(injector, it));
        }
    }

    private void registerInjector(Class<? extends InjectionInfo> injector, String packageName) {
        String name = injector.getAnnotation(InjectionInfo.AnnotationType.class).value().getName();
        String suffix = StringUtils.removeStart(name, ownPackage);
        MixinInternals.registerInjector(packageName + suffix, injector);
    }

    private void unregisterInjector(Class<? extends InjectionInfo> injector, String packageName) {
        String name = injector.getAnnotation(InjectionInfo.AnnotationType.class).value().getName();
        String suffix = StringUtils.removeStart(name, ownPackage);
        MixinInternals.unregisterInjector(packageName + suffix);
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

    /**
     * Detects and recognises active packages from versions between 0.2.0-beta.1 and 0.2.0-beta.9
     * We take over the handling of the sugar for these versions, but not the injectors.
     */
    private void detectBetaPackages() {
        for (IExtension extension : MixinInternals.getExtensions().getActiveExtensions()) {
            String name = extension.getClass().getName();
            String suffix = ".sugar.impl.SugarApplicatorExtension";
            if (name.endsWith(suffix) && !isClassOwned(name)) {
                // We have to assume this is from one of the offending versions.
                String packageName = StringUtils.removeEnd(name, suffix);
                allPackages.add(packageName);
                LOGGER.warn("Found problematic active MixinExtras instance at {}", packageName);
                LOGGER.warn("Versions from 0.2.0-beta.1 to 0.2.0-beta.9 have limited support and it is strongly recommended to update.");
            }
        }
    }
}
