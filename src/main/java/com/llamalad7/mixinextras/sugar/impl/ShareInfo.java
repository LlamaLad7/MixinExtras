package com.llamalad7.mixinextras.sugar.impl;

import com.llamalad7.mixinextras.injector.StackExtension;
import com.llamalad7.mixinextras.sugar.impl.ref.LocalRefUtils;
import com.llamalad7.mixinextras.utils.TargetDecorations;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.util.Annotations;

import java.util.*;

public class ShareInfo {
    private final int lvtIndex;
    private final ShareType shareType;
    private final MethodNode targetMethod;
    private final Collection<AbstractInsnNode> initialization = new ArrayList<>();

    private ShareInfo(int lvtIndex, Type innerType, MethodNode targetMethod) {
        this.lvtIndex = lvtIndex;
        this.shareType = new ShareType(innerType);
        this.targetMethod = targetMethod;
    }

    public void addToLvt(Target target) {
        shareType.addToLvt(target, lvtIndex);
    }

    public InsnList initialize() {
        InsnList init = shareType.initialize(lvtIndex);
        initialization.addAll(Arrays.asList(init.toArray()));
        return init;
    }

    public AbstractInsnNode load() {
        return new VarInsnNode(Opcodes.ALOAD, lvtIndex);
    }

    public static ShareInfo getOrCreate(Target target, AnnotationNode shareAnnotation, Type paramType, IMixinInfo mixin, StackExtension stack) {
        if (!SugarApplicator.isSugar(shareAnnotation.desc) || !shareAnnotation.desc.endsWith("Share;")) {
            return null;
        }
        Type innerType = getInnerType(paramType);
        Map<String, ShareInfo> infos = TargetDecorations.getOrPut(target, "ShareSugar_Infos", HashMap::new);
        String id = getId(shareAnnotation, mixin);
        ShareInfo shareInfo = infos.get(id);
        if (shareInfo == null) {
            shareInfo = new ShareInfo(target.allocateLocal(), innerType, target.method);
            infos.put(id, shareInfo);
            shareInfo.addToLvt(target);
            target.insns.insert(shareInfo.initialize());
            if (stack != null) {
                stack.ensureAtLeast(innerType.getSize() + 1); // ref and dummy value
            }
        } else {
            if (!innerType.equals(shareInfo.shareType.getInnerType())) {
                throw new SugarApplicationException(
                        String.format(
                                "Share id %s in %s was requested for different types %s and %s!",
                                id, target, innerType, shareInfo.shareType.getInnerType()
                        )
                );
            }
        }
        return shareInfo;
    }

    private static Type getInnerType(Type paramType) {
        Type innerType = LocalRefUtils.getTargetType(paramType, Type.getType(Object.class));
        if (innerType == paramType) {
            throw new SugarApplicationException("@Share parameter must be some variation of LocalRef.");
        }
        return innerType;
    }

    private static String getId(AnnotationNode shareAnnotation, IMixinInfo mixin) {
        return mixin.getClassRef() + ':' + Annotations.getValue(shareAnnotation);
    }
}
