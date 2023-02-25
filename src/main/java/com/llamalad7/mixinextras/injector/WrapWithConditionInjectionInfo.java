package com.llamalad7.mixinextras.injector;

import com.llamalad7.mixinextras.utils.Decorations;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo.HandlerPrefix;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;

import java.util.List;

@InjectionInfo.AnnotationType(WrapWithCondition.class)
@HandlerPrefix("wrapWithCondition")
public class WrapWithConditionInjectionInfo extends MixinExtrasInjectionInfo {
    public WrapWithConditionInjectionInfo(MixinTargetContext mixin, MethodNode method, AnnotationNode annotation) {
        super(mixin, method, annotation);
    }

    @Override
    protected Injector parseInjector(AnnotationNode injectAnnotation) {
        return new WrapWithConditionInjector(this);
    }

    @Override
    public void prepare() {
        super.prepare();
        for (List<InjectionNodes.InjectionNode> nodeList : this.targetNodes.values()) {
            for (InjectionNodes.InjectionNode node : nodeList) {
                AbstractInsnNode currentTarget = node.getCurrentTarget();
                if (currentTarget instanceof MethodInsnNode) {
                    Type returnType = Type.getReturnType(((MethodInsnNode) currentTarget).desc);
                    if (this.isTypePoppedByInstruction(returnType, currentTarget.getNext())) {
                        node.decorate(Decorations.POPPED_OPERATION, true);
                    }
                }
            }
        }
    }

    private boolean isTypePoppedByInstruction(Type type, AbstractInsnNode insn) {
        switch (type.getSize()) {
            case 2:
                return insn.getOpcode() == Opcodes.POP2;
            case 1:
                return insn.getOpcode() == Opcodes.POP;
            default:
                return false;
        }
    }
}
