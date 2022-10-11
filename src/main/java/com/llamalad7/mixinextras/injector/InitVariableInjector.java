package com.llamalad7.mixinextras.injector;

import com.llamalad7.mixinextras.utils.CompatibilityHelper;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.modify.InvalidImplicitDiscriminatorException;
import org.spongepowered.asm.mixin.injection.modify.LocalVariableDiscriminator;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes.InjectionNode;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.injection.throwables.InjectionError;

import java.util.List;

/**
 * @see org.spongepowered.asm.mixin.injection.modify.ModifyVariableInjector
 */
public class InitVariableInjector extends Injector {

    static class Context extends LocalVariableDiscriminator.Context {

        final InsnList insns = new InsnList();

        public Context(InjectionInfo info, Type returnType, boolean argsOnly, Target target, AbstractInsnNode node) {
            super(info, returnType, argsOnly, target, node);
        }
    }

    private final LocalVariableDiscriminator discriminator;

    public InitVariableInjector(InjectionInfo info, LocalVariableDiscriminator discriminator) {
        super(info, "@InitVariable");
        this.discriminator = discriminator;
    }

    protected String getTargetNodeKey(Target target, InjectionNode node) {
        return String.format("localcontext(%s,%s,#%s)", this.returnType, this.discriminator.isArgsOnly() ? "argsOnly" : "fullFrame", node.getId());
    }

    @Override
    protected void preInject(Target target, InjectionNode injectionNode) {
        String key = this.getTargetNodeKey(target, injectionNode);
        if (injectionNode.hasDecoration(key)) {
            return; // already have a suitable context
        }

        // We go to the next frame to get the locals there, since
        // the uninitialized variable is probably not present yet
        AbstractInsnNode node = injectionNode.getCurrentTarget();
        while(node != null && !(node instanceof FrameNode)) {
            if(node instanceof JumpInsnNode) {
                node = ((JumpInsnNode) node).label;
            }
            node = node.getNext();
        }

        Context context = new Context(this.info, this.returnType, this.discriminator.isArgsOnly(), target, node);
        injectionNode.decorate(key, context);
    }

    @Override
    protected void inject(Target target, InjectionNode node) {
        if (node.isReplaced()) {
            throw CompatibilityHelper.makeInvalidInjectionException(this.info,
                String.format(
                        "Target of %s annotation was removed by another injector in %s in %s",
                        this.annotationType, target, this
                ));
        }

        Context context = node.getDecoration(this.getTargetNodeKey(target, node));
        if (context == null) {
            throw new InjectionError(String.format(
                    "%s injector target is missing CONTEXT decoration for %s. PreInjection failure or illegal internal state change",
                    this.annotationType, this.info));
        }

        // If the context is being reused (because two identical injectors are targetting this node)
        // then the insns SHOULD have been drained by the previous insertBefore. If the list hasn't
        // been cleared for some reason then something probably went wrong during the previous inject
        if (context.insns.size() > 0) {
            throw new InjectionError(String.format(
                    "%s injector target has contaminated CONTEXT decoration for %s. Check for previous errors.",
                    this.annotationType, this.info));
        }

        this.checkTargetForNode(target, node, InjectionPoint.RestrictTargetLevel.ALLOW_ALL);

        InjectorData handler = new InjectorData(target, "handler", false);

        if (this.returnType == Type.VOID_TYPE) {
            throw CompatibilityHelper.makeInvalidInjectionException(this.info,
                    String.format(
                            "%s %s method %s from %s has invalid signature, cannot return a VOID type.",
                            this.annotationType, handler, this, this.info.getMixin()
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
        target.insns.insertBefore(node.getCurrentTarget(), context.insns);
    }

    private void inject(Context context, InjectorData handler, Target.Extension extraStack, int local) {
        if (!this.isStatic) {
            context.insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
            extraStack.add();
        }

        // Unlike ModifyVariable, SetVariable doesn't load the previous value

        if (handler.captureTargetArgs > 0) {
            this.pushArgs(handler.target.arguments, context.insns, handler.target.getArgIndices(), 0, handler.captureTargetArgs, extraStack);
        }

        this.invokeHandler(context.insns);
        context.insns.add(new VarInsnNode(this.returnType.getOpcode(Opcodes.ISTORE), local));
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
