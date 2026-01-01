package com.llamalad7.mixinextras.injector.wrapmethod;

import com.llamalad7.mixinextras.sugar.impl.ShareInfo;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.transformer.ext.IExtension;
import org.spongepowered.asm.mixin.transformer.ext.ITargetClassContext;

import java.util.*;

public class WrapMethodApplicatorExtension implements IExtension {
    private static final Map<ClassNode, Map<MethodNode, WrapMethodStage>> wrappers = new HashMap<>();

    static void offerWrapper(Target target, MethodNode handler, Type operationType, List<ShareInfo> shares, boolean captureParams) {
        Map<MethodNode, WrapMethodStage> relevant = wrappers.computeIfAbsent(target.classNode, k -> new LinkedHashMap<>());
        WrapMethodStage inner = relevant.computeIfAbsent(target.method, WrapMethodStage.Vanilla::new);
        relevant.put(target.method, new WrapMethodStage.Wrapper(inner, handler, operationType, shares, captureParams));
    }

    @Override
    public boolean checkActive(MixinEnvironment environment) {
        return true;
    }

    @Override
    public void preApply(ITargetClassContext context) {
    }

    @Override
    public void postApply(ITargetClassContext context) {
        ClassNode targetClass = context.getClassNode();
        Map<MethodNode, WrapMethodStage> relevant = wrappers.get(targetClass);
        if (relevant == null) {
            return;
        }
        for (WrapMethodStage wrapper : relevant.values()) {
            wrapper.apply(targetClass, new LinkedHashSet<>());
        }

        wrappers.remove(targetClass);
    }

    @Override
    public void export(MixinEnvironment env, String name, boolean force, ClassNode classNode) {
    }
}
