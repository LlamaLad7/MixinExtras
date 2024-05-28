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
    private int lvtIndex;
    private final ShareType shareType;
    private final Collection<AbstractInsnNode> initialization = new ArrayList<>();

    private ShareInfo(int lvtIndex, Type innerType) {
        this.lvtIndex = lvtIndex;
        this.shareType = new ShareType(innerType);
    }

    public int getLvtIndex() {
        return lvtIndex;
    }

    public void setLvtIndex(int lvtIndex) {
        this.lvtIndex = lvtIndex;
    }

    public ShareType getShareType() {
        return shareType;
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

    public void stripInitializerFrom(MethodNode method) {
        initialization.forEach(method.instructions::remove);
    }

    public static ShareInfo getOrCreate(Target target, AnnotationNode shareAnnotation, Type paramType, IMixinInfo mixin, StackExtension stack) {
        if (!SugarApplicator.isSugar(shareAnnotation.desc) || !shareAnnotation.desc.endsWith("Share;")) {
            return null;
        }
        Type innerType = getInnerType(paramType);
        Map<ShareId, ShareInfo> infos = TargetDecorations.getOrPut(target, "ShareSugar_Infos", HashMap::new);
        ShareId id = getId(shareAnnotation, mixin);
        ShareInfo shareInfo = infos.get(id);
        if (shareInfo == null) {
            shareInfo = new ShareInfo(target.allocateLocal(), innerType);
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

    private static ShareId getId(AnnotationNode shareAnnotation, IMixinInfo mixin) {
        return new ShareId(
                Annotations.getValue(shareAnnotation, "namespace", mixin.getClassName()),
                Annotations.getValue(shareAnnotation)
        );
    }

    private static class ShareId {
        private final String namespace;
        private final String id;

        private ShareId(String namespace, String id) {
            this.namespace = namespace;
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ShareId shareId = (ShareId) o;
            return Objects.equals(namespace, shareId.namespace) && Objects.equals(id, shareId.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(namespace, id);
        }
    }
}
