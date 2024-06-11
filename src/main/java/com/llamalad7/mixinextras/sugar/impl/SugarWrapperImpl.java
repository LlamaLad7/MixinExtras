package com.llamalad7.mixinextras.sugar.impl;

import com.llamalad7.mixinextras.sugar.SugarBridge;
import com.llamalad7.mixinextras.sugar.impl.handlers.HandlerInfo;
import com.llamalad7.mixinextras.utils.CompatibilityHelper;
import com.llamalad7.mixinextras.utils.GenericParamParser;
import com.llamalad7.mixinextras.utils.MixinInternals;
import com.llamalad7.mixinextras.wrapper.InjectorWrapperImpl;
import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.injection.throwables.InjectionError;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.asm.MethodNodeEx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SugarWrapperImpl extends InjectorWrapperImpl {
    private final InjectionInfo wrapperInfo;
    private final AnnotationNode originalAnnotation;
    private final List<AnnotationNode> sugarAnnotations;
    private final ArrayList<Type> generics;
    private final MethodNode handler;
    private final InjectionInfo delegate;
    private final SugarInjector sugarInjector;

    protected SugarWrapperImpl(InjectionInfo wrapper, MixinTargetContext mixin, MethodNode method, AnnotationNode annotation) {
        super(wrapper, mixin, method, annotation, true);
        wrapperInfo = wrapper;
        method.visibleAnnotations.remove(annotation);
        method.visibleAnnotations.add(originalAnnotation = Annotations.getValue(annotation, "original"));
        sugarAnnotations = Annotations.getValue(annotation, "sugars");
        generics = new ArrayList<>(
                GenericParamParser.getParameterGenerics(method.desc, Annotations.getValue(annotation, "signature"))
        );
        handler = prepareHandler(method);
        sugarInjector = new SugarInjector(wrapperInfo, mixin.getMixin(), handler, sugarAnnotations, generics);
        sugarInjector.stripSugar();
        delegate = InjectionInfo.parse(mixin, handler);
        sugarInjector.setTargets(MixinInternals.getTargets(delegate));
        if (!isValid()) {
            // The injector is now dropped by mixin, so we must make sure the handler method is in a valid state.
            sugarInjector.reSugarHandler();
        }
    }

    @Override
    protected InjectionInfo getDelegate() {
        return delegate;
    }

    @Override
    protected MethodNode getHandler() {
        return handler;
    }

    @Override
    protected void prepare() {
        super.prepare();
        sugarInjector.prepareSugar();
    }

    @Override
    protected void granularInject(HandlerCallCallback callback) {
        Map<Target, List<Pair<InjectionNodes.InjectionNode, MethodInsnNode>>> handlerCallMap = new HashMap<>();
        super.granularInject(((target, sourceNode, call) -> {
            callback.onFound(target, sourceNode, call);
            handlerCallMap.computeIfAbsent(target, k -> new ArrayList<>()).add(Pair.of(sourceNode, call));
        }));
        sugarInjector.reSugarHandler();
        sugarInjector.transformHandlerCalls(handlerCallMap);
    }

    @Override
    protected void doPostInject(Runnable postInject) {
        try {
            super.doPostInject(postInject);
        } catch (InvalidInjectionException | InjectionError e) {
            for (SugarApplicationException sugarException : sugarInjector.getExceptions()) {
                e.addSuppressed(sugarException);
            }
            throw e;
        }
    }

    private MethodNode prepareHandler(MethodNode original) {
        IMixinInfo mixin = CompatibilityHelper.getMixin(wrapperInfo).getMixin();
        HandlerInfo handlerInfo = SugarInjector.getHandlerInfo(mixin, original, sugarAnnotations, generics);
        if (handlerInfo == null) {
            return original;
        }
        MethodNodeEx newMethod = new MethodNodeEx(
                original.access, MethodNodeEx.getName(original), original.desc, original.signature,
                original.exceptions.toArray(new String[0]), mixin);
        original.accept(newMethod);
        original.visibleAnnotations.remove(originalAnnotation);
        newMethod.name = original.name;
        newMethod.tryCatchBlocks = null;
        newMethod.visitAnnotation(Type.getDescriptor(SugarBridge.class), false);
        handlerInfo.transformHandler(classNode, newMethod);
        handlerInfo.transformGenerics(generics);
        classNode.methods.add(newMethod);
        return newMethod;
    }
}
