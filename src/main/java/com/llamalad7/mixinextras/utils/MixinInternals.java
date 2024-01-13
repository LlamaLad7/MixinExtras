package com.llamalad7.mixinextras.utils;

import com.llamalad7.mixinextras.wrapper.WrapperInjectionInfo;
import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes.InjectionNode;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.transformer.ClassInfo;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.mixin.transformer.ext.Extensions;
import org.spongepowered.asm.mixin.transformer.ext.IExtension;
import org.spongepowered.asm.mixin.transformer.ext.IExtensionRegistry;
import org.spongepowered.asm.mixin.transformer.ext.ITargetClassContext;
import org.spongepowered.asm.mixin.transformer.ext.extensions.ExtensionCheckClass;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.function.Predicate;

/**
 * Mumfrey, look away.
 */
@SuppressWarnings("unchecked")
public class MixinInternals {
    private static final InternalField<ITargetClassContext, SortedSet<IMixinInfo>> TARGET_CLASS_CONTEXT_MIXINS
            = InternalField.of("org.spongepowered.asm.mixin.transformer.TargetClassContext", "mixins");
    private static final InternalMethod<IMixinInfo, ?> MIXIN_INFO_GET_STATE
            = InternalMethod.of("org.spongepowered.asm.mixin.transformer.MixinInfo", "getState");
    private static final InternalField<Object, ClassNode> STATE_CLASS_NODE
            = InternalField.of("org.spongepowered.asm.mixin.transformer.MixinInfo$State", "classNode");
    private static final InternalField<IExtensionRegistry, List<IExtension>> EXTENSIONS
            = InternalField.of(Extensions.class, "extensions");
    private static final InternalField<IExtensionRegistry, List<IExtension>> ACTIVE_EXTENSIONS
            = InternalField.of(Extensions.class, "activeExtensions");
    private static final InternalField<InjectionInfo, Map<Target, List<InjectionNode>>> INJECTION_INFO_TARGET_NODES
            = InternalField.of(InjectionInfo.class, "targetNodes");
    private static final InternalField<InjectionNode, Map<String, Object>> INJECTION_NODE_DECORATIONS
            = InternalField.of(InjectionNode.class, "decorations");
    private static final InternalField<InjectionInfo, Injector> INJECTION_INFO_INJECTOR
            = InternalField.of(InjectionInfo.class, "injector");
    private static final InternalMethod<?, Void> CLASS_INFO_FROM_CLASS_NODE
            = InternalMethod.of(ClassInfo.class, "fromClassNode", ClassNode.class);
    private static final InternalConstructor<?> INJECTOR_ENTRY
            = InternalConstructor.of("org.spongepowered.asm.mixin.injection.struct.InjectionInfo$InjectorEntry", Class.class, Class.class);
    private static final InternalField<Object, Class<? extends Annotation>> INJECTOR_ENTRY_ANNOTATION_TYPE
            = InternalField.of("org.spongepowered.asm.mixin.injection.struct.InjectionInfo$InjectorEntry", "annotationType");
    private static final InternalField<?, Map<String, ?>> INJECTION_INFO_REGISTRY
            = InternalField.of(InjectionInfo.class, "registry");
    private static final InternalField<InjectionInfo, Class<? extends Annotation>[]> INJECTION_INFO_REGISTERED_ANNOTATIONS
            = InternalField.of(InjectionInfo.class, "registeredAnnotations");
    private static final InternalField<?, Map<String, Class<? extends InjectionPoint>>> INJECTION_POINT_TYPES
            = InternalField.of(InjectionPoint.class, "types");

    public static List<Pair<IMixinInfo, ClassNode>> getMixinsFor(ITargetClassContext context) {
        List<Pair<IMixinInfo, ClassNode>> result = new ArrayList<>();
        for (IMixinInfo mixin : TARGET_CLASS_CONTEXT_MIXINS.get(context)) {
            result.add(Pair.of(mixin, getClassNode(mixin)));
        }
        return result;
    }

    public static Map<Target, List<InjectionNode>> getTargets(InjectionInfo info) {
        if (info instanceof WrapperInjectionInfo) {
            return ((WrapperInjectionInfo) info).getTargetMap();
        }
        return INJECTION_INFO_TARGET_NODES.get(info);
    }

    public static Extensions getExtensions() {
        IMixinTransformer transformer = (IMixinTransformer) MixinEnvironment.getDefaultEnvironment().getActiveTransformer();
        return (Extensions) transformer.getExtensions();
    }

    public static void registerExtension(IExtension extension) {
        registerExtension(extension, false);
    }

    public static void registerExtension(IExtension extension, boolean isPriority) {
        IExtensionRegistry extensions = getExtensions();
        List<IExtension> extensionsList = EXTENSIONS.get(extensions);
        addExtension(extensionsList, extension, isPriority);
        List<IExtension> activeExtensions = new ArrayList<>(ACTIVE_EXTENSIONS.get(extensions));
        addExtension(activeExtensions, extension, isPriority);
        ACTIVE_EXTENSIONS.set(extensions, Collections.unmodifiableList(activeExtensions));
    }

    public static void unregisterExtension(IExtension extension) {
        Extensions extensions = getExtensions();
        List<IExtension> extensionsList = EXTENSIONS.get(extensions);
        extensionsList.remove(extension);
        List<IExtension> activeExtensions = new ArrayList<>(ACTIVE_EXTENSIONS.get(extensions));
        activeExtensions.remove(extension);
        ACTIVE_EXTENSIONS.set(extensions, Collections.unmodifiableList(activeExtensions));
    }

    private static void addExtension(List<IExtension> extensions, IExtension newExtension, boolean isPriority) {
        if (isPriority) {
            extensions.add(0, newExtension);
        } else {
            extensions.add(newExtension);
        }
        // If this runs before our extensions it will fail since we're not done generating our bytecode.
        shiftLateExtensions(extensions, it -> it instanceof ExtensionCheckClass);
    }

    private static void shiftLateExtensions(List<IExtension> extensions, Predicate<IExtension> isLate) {
        List<IExtension> lateExtensions = new ArrayList<>();
        for (ListIterator<IExtension> it = extensions.listIterator(); it.hasNext(); ) {
            IExtension extension = it.next();
            if (isLate.test(extension)) {
                it.remove();
                lateExtensions.add(extension);
            }
        }
        extensions.addAll(lateExtensions);
    }

    public static Map<String, Object> getDecorations(InjectionNode node) {
        Map<String, Object> result = INJECTION_NODE_DECORATIONS.get(node);
        return result == null ? Collections.emptyMap() : result;
    }

    public static Injector getInjector(InjectionInfo info) {
        return INJECTION_INFO_INJECTOR.get(info);
    }

    public static ClassNode getClassNode(IMixinInfo mixin) {
        return STATE_CLASS_NODE.get(MIXIN_INFO_GET_STATE.call(mixin));
    }

    public static void registerClassInfo(ClassNode classNode) {
        CLASS_INFO_FROM_CLASS_NODE.call(null, classNode);
    }

    public static void registerInjector(String annotationType, Class<?> type) {
        Class<?> clazz;
        try {
            clazz = Class.forName(annotationType);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Could not find injector annotation, please report to LlamaLad7!", e);
        }
        Map<String, Object> registry = (Map<String, Object>) INJECTION_INFO_REGISTRY.get(null);
        Object entry = INJECTOR_ENTRY.newInstance(clazz, type);

        registry.put(Type.getDescriptor(clazz), entry);
        bakeInjectionInfoArray(registry);
    }

    public static void unregisterInjector(String annotationType) {
        Map<String, Object> registry = (Map<String, Object>) INJECTION_INFO_REGISTRY.get(null);
        registry.remove('L' + annotationType.replace('.', '/') + ';');
        bakeInjectionInfoArray(registry);
    }

    private static void bakeInjectionInfoArray(Map<String, Object> registry) {
        List<Class<? extends Annotation>> annotations = new ArrayList<>();
        for (Object injector : registry.values()) {
            annotations.add(INJECTOR_ENTRY_ANNOTATION_TYPE.get(injector));
        }
        INJECTION_INFO_REGISTERED_ANNOTATIONS.set(null, annotations.toArray(new Class[0]));
    }

    public static void registerInjectionPoint(Class<? extends InjectionPoint> point) {
        String code = point.getAnnotation(InjectionPoint.AtCode.class).value();
        INJECTION_POINT_TYPES.get(null).put(code, point);
    }
}
