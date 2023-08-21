package com.llamalad7.mixinextras.sugar.impl;

import com.llamalad7.mixinextras.injector.LateApplyingInjectorInfo;
import com.llamalad7.mixinextras.sugar.SugarBridge;
import com.llamalad7.mixinextras.sugar.impl.handlers.HandlerInfo;
import com.llamalad7.mixinextras.utils.CompatibilityHelper;
import com.llamalad7.mixinextras.utils.GenericParamParser;
import com.llamalad7.mixinextras.utils.MixinInternals;
import com.llamalad7.mixinextras.utils.ProxyUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo.AnnotationType;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo.HandlerPrefix;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes.InjectionNode;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.injection.throwables.InjectionError;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.asm.MethodNodeEx;

import java.util.*;

@AnnotationType(SugarWrapper.class)
@HandlerPrefix("sugarWrapper")
public class SugarWrapperInjectionInfo extends InjectionInfo implements LateApplyingInjectorInfo {
    private final AnnotationNode originalAnnotation;
    private final List<AnnotationNode> sugarAnnotations;
    private final ArrayList<Type> generics;
    private final MethodNode handler;
    private final InjectionInfo delegate;
    private final SugarInjector sugarInjector;
    private final boolean lateApply;

    public SugarWrapperInjectionInfo(MixinTargetContext mixin, MethodNode method, AnnotationNode annotation) {
        super(mixin, method, annotation);
        method.visibleAnnotations.remove(annotation);
        method.visibleAnnotations.add(originalAnnotation = Annotations.getValue(annotation, "original"));
        sugarAnnotations = Annotations.getValue(annotation, "sugars");
        generics = new ArrayList<>(
                GenericParamParser.getParameterGenerics(method.desc, Annotations.getValue(annotation, "signature"))
        );
        handler = prepareHandler(method);
        sugarInjector = new SugarInjector(this, mixin.getMixin(), handler, sugarAnnotations, generics);
        sugarInjector.stripSugar();
        delegate = InjectionInfo.parse(mixin, handler);
        sugarInjector.setTargets(MixinInternals.getTargets(delegate));
        lateApply = LateApplyingInjectorInfo.wrap(delegate, this);
        if (!lateApply) {
            checkDelegate();
        }
    }

    @Override
    protected void readAnnotation() {
    }

    @Override
    protected Injector parseInjector(AnnotationNode injectAnnotation) {
        return null;
    }

    @Override
    public boolean isValid() {
        return delegate.isValid();
    }

    @Override
    public void prepare() {
        delegate.prepare();
        handler.visibleAnnotations.remove(InjectionInfo.getInjectorAnnotation(CompatibilityHelper.getMixin(this).getMixin(), handler));
        sugarInjector.prepareSugar();
    }

    // @Override on 0.8.3+
    @SuppressWarnings("unused")
    public void preInject() {
        CompatibilityHelper.preInject(delegate);
    }

    @Override
    public void inject() {
        if (lateApply) {
            delegate.inject();
        } else {
            doInject();
        }
    }

    private void doInject() {
        Map<Target, List<InjectionNode>> targets = MixinInternals.getTargets(delegate);
        Injector injector = MixinInternals.getInjector(delegate);
        Map<Target, List<Pair<InjectionNode, MethodInsnNode>>> handlerCallMap = new HashMap<>();
        for (Map.Entry<Target, List<InjectionNode>> entry : targets.entrySet()) {
            Target target = entry.getKey();
            List<Pair<InjectionNode, MethodInsnNode>> handlerCalls = new ArrayList<>();
            handlerCallMap.put(target, handlerCalls);

            Set<MethodInsnNode> discoveredHandlerCalls = new HashSet<>(findHandlerCalls(target));
            for (InjectionNode node : entry.getValue()) {
                inject(injector, target, node);
                for (MethodInsnNode handlerCall : findHandlerCalls(target)) {
                    if (discoveredHandlerCalls.add(handlerCall)) {
                        handlerCalls.add(Pair.of(node, handlerCall));
                    }
                }
            }
            postInject(injector, target, entry.getValue());
        }
        targets.clear();

        sugarInjector.reSugarHandler();
        sugarInjector.transformHandlerCalls(handlerCallMap);
    }

    @Override
    public void postInject() {
        if (!lateApply) {
            doPostInject(delegate::postInject);
        }
    }

    private void doPostInject(Runnable postInject) {
        try {
            postInject.run();
        } catch (InvalidInjectionException | InjectionError e) {
            for (SugarApplicationException sugarException : sugarInjector.getExceptions()) {
                e.addSuppressed(sugarException);
            }
            throw e;
        }
    }

    @Override
    public void addCallbackInvocation(MethodNode handler) {
        delegate.addCallbackInvocation(handler);
    }

    @Override
    public void lateInject() {
        doInject();
    }

    @Override
    public void latePostInject() {
        doPostInject(ProxyUtils.getProxy(delegate, LateApplyingInjectorInfo.class)::latePostInject);
    }

    @Override
    public void wrap(LateApplyingInjectorInfo outer) {
        throw new UnsupportedOperationException("Cannot wrap a sugar wrapper!");
    }

    private MethodNode prepareHandler(MethodNode original) {
        IMixinInfo mixin = CompatibilityHelper.getMixin(this).getMixin();
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

    private void checkDelegate() {
        try {
            if (delegate.getClass().getMethod("inject").getDeclaringClass() != InjectionInfo.class) {
                throw new UnsupportedOperationException(
                        delegate.getClass() + " overrides 'inject' and so is not automatically compatible with Sugar." +
                                " Please report to LlamaLad7 in case manual compatibility can be added."
                );
            }
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private List<MethodInsnNode> findHandlerCalls(Target target) {
        List<MethodInsnNode> result = new ArrayList<>();
        for (AbstractInsnNode insn : target) {
            if (insn instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) insn;
                if (call.owner.equals(classNode.name) && call.name.equals(handler.name) && call.desc.equals(handler.desc)) {
                    result.add(call);
                }
            }
        }
        return result;
    }

    private static void inject(Injector injector, Target target, InjectionNode node) {
        injector.inject(target, new SingleIterationList<>(Collections.singletonList(node), 0));
    }

    private static void postInject(Injector injector, Target target, List<InjectionNode> nodes) {
        injector.inject(target, new SingleIterationList<>(nodes, 1));
    }
}
