package com.llamalad7.mixinextras.sugar.impl;

import com.llamalad7.mixinextras.injector.LateApplyingInjectorInfo;
import com.llamalad7.mixinextras.utils.CompatibilityHelper;
import com.llamalad7.mixinextras.utils.MixinInternals;
import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
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

import java.util.*;

@AnnotationType(SugarWrapper.class)
@HandlerPrefix("sugarWrapper")
public class SugarWrapperInjectionInfo extends InjectionInfo implements LateApplyingInjectorInfo {
    private final InjectionInfo delegate;
    private final AnnotationNode originalAnnotation;
    private final SugarInjector sugarInjector;
    private final boolean lateApply;

    public SugarWrapperInjectionInfo(MixinTargetContext mixin, MethodNode method, AnnotationNode annotation) {
        super(mixin, method, annotation);
        sugarInjector = new SugarInjector(this, mixin.getMixin(), method);
        method.visibleAnnotations.remove(annotation);
        method.visibleAnnotations.add(originalAnnotation = Annotations.getValue(annotation, "original"));
        sugarInjector.stripSugar();
        delegate = InjectionInfo.parse(mixin, method);
        sugarInjector.setTargets(MixinInternals.getTargets(delegate));
        lateApply = delegate instanceof LateApplyingInjectorInfo;
        if (lateApply) {
            ((LateApplyingInjectorInfo) delegate).wrap(this);
        }
        checkDelegate();
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
        method.visibleAnnotations.remove(originalAnnotation);
        sugarInjector.prepareSugar();
    }

    // @Override on 0.8.3+
    @SuppressWarnings("unused")
    public void preInject() {
        CompatibilityHelper.preInject(delegate);
    }

    @Override
    public void inject() {
        // TODO lateApply
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
        try {
            delegate.postInject();
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
    public void lateApply() {
        try {
            ((LateApplyingInjectorInfo) delegate).lateApply();
        } catch (InvalidInjectionException | InjectionError e) {
            for (SugarApplicationException sugarException : sugarInjector.getExceptions()) {
                e.addSuppressed(sugarException);
            }
            throw e;
        }
//        sugarInjector.applySugar();
    }

    @Override
    public void wrap(LateApplyingInjectorInfo outer) {
        throw new UnsupportedOperationException("Cannot wrap a sugar wrapper!");
    }

    private void checkDelegate() {
        try {
            if (delegate.getClass().getMethod("inject").getDeclaringClass() != InjectionInfo.class) {
                throw new UnsupportedOperationException(
                        delegate.getClass() + "overrides 'inject' and so is not automatically compatible with Sugar." +
                                "Please report to LlamaLad7 in case manual compatibility can be added."
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
                if (call.owner.equals(classNode.name) && call.name.equals(method.name) && call.desc.equals(method.desc)) {
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
