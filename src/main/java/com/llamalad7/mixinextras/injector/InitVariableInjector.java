package com.llamalad7.mixinextras.injector;

import com.llamalad7.mixinextras.utils.CompatibilityHelper;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.modify.InvalidImplicitDiscriminatorException;
import org.spongepowered.asm.mixin.injection.modify.LocalVariableDiscriminator;
import org.spongepowered.asm.mixin.injection.modify.LocalVariableDiscriminator.Context;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes.InjectionNode;
import org.spongepowered.asm.mixin.injection.struct.Target;

import java.util.List;

/**
 * @see org.spongepowered.asm.mixin.injection.modify.ModifyVariableInjector
 */
public class InitVariableInjector extends Injector {

    private final LocalVariableDiscriminator discriminator;

    public InitVariableInjector(InjectionInfo info, LocalVariableDiscriminator discriminator) {
        super(info, "@InitVariable");
        this.discriminator = discriminator;
    }

    @Override
    protected void inject(Target target, InjectionNode injectionNode) {

        // We go to the next frame to get the locals there, since
        // the uninitialized variable is probably not present yet
        AbstractInsnNode node = injectionNode.getCurrentTarget();
        while(node != null && !(node instanceof FrameNode)) {
            if(node instanceof JumpInsnNode) {
                node = ((JumpInsnNode) node).label;
            }
            node = node.getNext();
        }

        Context context = CompatibilityHelper.createModifyVariableContext(this.info, this.returnType, this.discriminator.isArgsOnly(), target, node);

        this.checkTargetForNode(target, injectionNode, InjectionPoint.RestrictTargetLevel.ALLOW_ALL);

        InjectorData handler = new InjectorData(target, "handler", false);

        if (this.returnType == Type.VOID_TYPE) {
            throw CompatibilityHelper.makeInvalidInjectionException(this.info,
                    String.format(
                            "%s %s method %s from %s has invalid signature, cannot return a VOID type.",
                            this.annotationType, handler, this, CompatibilityHelper.getMixin(info)
                    ));
        }

        this.validateParams(handler, this.returnType);

        Target.Extension extraStack = target.extendStack();

        try {
            int local = this.discriminator.findLocal(context);
            if (local > -1) {
                this.inject(context, handler, extraStack, local);
            }
        } catch (InvalidImplicitDiscriminatorException ex) {
            throw CompatibilityHelper.makeInvalidInjectionException(this.info,
                    String.format(
                            "%s implicit variable setter injection failed in %s in %s",
                            this.annotationType, target, this
                    ));
        }

        extraStack.apply();
        target.insns.insertBefore(injectionNode.getCurrentTarget(), CompatibilityHelper.getModifyVariableContextInsns(context));
    }

    private void inject(Context context, InjectorData handler, Target.Extension extraStack, int local) {
        InsnList insns = CompatibilityHelper.getModifyVariableContextInsns(context);
        if (!this.isStatic) {
            insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
            extraStack.add();
        }

        // Unlike ModifyVariable, SetVariable doesn't load the previous value

        if (handler.captureTargetArgs > 0) {
            this.pushArgs(handler.target.arguments, insns, handler.target.getArgIndices(), 0, handler.captureTargetArgs, extraStack);
        }

        this.invokeHandler(insns);
        insns.add(new VarInsnNode(this.returnType.getOpcode(Opcodes.ISTORE), local));
    }

    @Override
    protected void sanityCheck(Target target, List<InjectionPoint> injectionPoints) {
        super.sanityCheck(target, injectionPoints);

        int ordinal = this.discriminator.getOrdinal();
        if (ordinal < -1) {
            throw CompatibilityHelper.makeInvalidInjectionException(this.info,
                    String.format(
                            "%s annotation has invalid ordinal %s specified in %s in %s",
                            this.annotationType, ordinal, target, this
                    ));
        }

        if (this.discriminator.getIndex() == 0 && !this.isStatic) {
            throw CompatibilityHelper.makeInvalidInjectionException(this.info,
                    String.format(
                        "%s annotation has invalid index 0 specified in non-static variable setter in %s in %s",
                        this.annotationType, target, this
            ));
        }
    }
}
