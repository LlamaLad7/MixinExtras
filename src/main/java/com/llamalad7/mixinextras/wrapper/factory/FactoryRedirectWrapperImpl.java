package com.llamalad7.mixinextras.wrapper.factory;

import com.llamalad7.mixinextras.utils.MixinInternals;
import com.llamalad7.mixinextras.wrapper.InjectorWrapperImpl;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes.InjectionNode;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;
import org.spongepowered.asm.util.Annotations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class FactoryRedirectWrapperImpl extends InjectorWrapperImpl {
    private final InjectionInfo delegate;
    private final MethodNode handler;

    protected FactoryRedirectWrapperImpl(InjectionInfo wrapper, MixinTargetContext mixin, MethodNode method, AnnotationNode annotation) {
        super(wrapper, mixin, method, annotation, true);
        method.visibleAnnotations.remove(annotation);
        method.visibleAnnotations.add(Annotations.getValue(annotation, "original"));
        handler = method;
        delegate = InjectionInfo.parse(mixin, method);
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
    protected void granularInject(HandlerCallCallback callback) {
        Map<InjectionNode, List<InjectionNode>> replacements = new HashMap<>();
        for (Map.Entry<Target, List<InjectionNode>> entry : MixinInternals.getTargets(delegate).entrySet()) {
            for (InjectionNode source : entry.getValue()) {
                findReplacedNodes(entry.getKey(), source, it -> replacements.computeIfAbsent(source, k -> new ArrayList<>()).add(it));
            }
        }
        super.granularInject(((target, sourceNode, call) -> {
            callback.onFound(target, sourceNode, call);
            List<InjectionNode> replacedNodes = replacements.get(sourceNode);
            if (replacedNodes == null) return;
            for (InjectionNode replaced : replacedNodes) {
                replaced.replace(call);
            }
        }));
    }

    private void findReplacedNodes(Target target, InjectionNode source, Consumer<InjectionNode> sink) {
        if (source.isRemoved() || source.getCurrentTarget().getOpcode() != Opcodes.NEW) return;
        sink.accept(source);
        sink.accept(target.addInjectionNode(target.findInitNodeFor((TypeInsnNode) source.getCurrentTarget())));
    }
}
