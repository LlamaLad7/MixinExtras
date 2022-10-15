package com.llamalad7.mixinextras.injector;

import com.llamalad7.mixinextras.utils.CompatibilityHelper;
import com.llamalad7.mixinextras.utils.InjectorUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes.InjectionNode;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.util.Annotations;

import java.util.List;
import java.util.Set;

public class RedirectExitInjector extends Injector {

    class Meta {

        public static final String KEY = "mixinextras_exitRedirector";

        final int priority;

        final boolean isFinal;

        final String name;

        final String desc;

        public Meta(int priority, boolean isFinal, String name, String desc) {
            this.priority = priority;
            this.isFinal = isFinal;
            this.name = name;
            this.desc = desc;
        }

        RedirectExitInjector getOwner() {
            return RedirectExitInjector.this;
        }

    }

    private final Meta meta;

    public RedirectExitInjector(InjectionInfo info) {
        super(info, "@RedirectExit");

        int priority = CompatibilityHelper.getMixin(info).getPriority();
        boolean isFinal = Annotations.getVisible(this.methodNode, Final.class) != null;
        this.meta = new Meta(priority, isFinal, this.info.toString(), this.methodNode.desc);
    }

    @Override
    protected void addTargetNode(Target target, List<InjectionNode> myNodes, AbstractInsnNode insn, Set<InjectionPoint> nominators) {
        InjectionNode node = target.getInjectionNode(insn);

        if (node != null ) {
            Meta other = node.getDecoration(Meta.KEY);

            if (other != null && other.getOwner() != this) {
                if (other.priority >= this.meta.priority) {
                    CompatibilityHelper.injectorLoggerWarn("{} conflict. Skipping {} with priority {}, already redirected by {} with priority {}",
                            this.annotationType, this.info, this.meta.priority, other.name, other.priority);
                    return;
                } else if (other.isFinal) {
                    throw CompatibilityHelper.makeInvalidInjectionException(this.info,
                            String.format(
                                    "%s conflict: %s failed because target was already remapped by %s",
                                    this.annotationType, this, other.name
                            ));
                }
            }
        }

        InjectionNode targetNode = target.addInjectionNode(insn);
        targetNode.decorate(Meta.KEY, this.meta);
        myNodes.add(targetNode);
    }

    @Override
    protected void inject(Target target, InjectionNode node) {
        if (!this.preInject(node)) {
            return;
        }

        if (node.isReplaced()) {
            throw new UnsupportedOperationException("Exit-Redirector target failure for " + this.info);
        }
        this.checkTargetModifiers(target, false);
        InjectorData data = new InjectorData(target, "exit redirector");
        int opcode = node.getOriginalTarget().getOpcode();
        boolean hasParameter = false;
        if(opcode == Opcodes.ATHROW) {
            this.validateParams(data, Type.VOID_TYPE, Type.getType(Throwable.class));
            hasParameter = true;
        } else if(opcode >= Opcodes.IRETURN && opcode <= Opcodes.ARETURN) {
            this.validateParams(data, Type.VOID_TYPE, target.returnType);
            hasParameter = true;
        } else if(opcode == Opcodes.RETURN) {
            this.validateParams(data, Type.VOID_TYPE);
        } else {
            throw CompatibilityHelper.makeInvalidInjectionException(this.info,
                    String.format(
                            "%s annotation on is targeting an invalid insn in %s in %s",
                            this.annotationType, target, this
                    ));
        }

        if(!InjectorUtils.isExitOptional(node.getCurrentTarget(), target)) {
            throw CompatibilityHelper.makeInvalidInjectionException(this.info,
                    String.format(
                            "%s annotation is targeting a method exiting instruction that is not safe to remove in %s in %s",
                            this.annotationType, target, this
                    ));
        }

        InsnList insns = new InsnList();
        if(!target.isStatic) {
            insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
            if(hasParameter) {
                insns.add(new InsnNode(Opcodes.SWAP));
            }
        }
        AbstractInsnNode handler = this.invokeHandler(insns);
        target.replaceNode(node.getCurrentTarget(), handler, insns);
    }

    protected boolean preInject(InjectionNode node) {
        Meta other = node.getDecoration(Meta.KEY);
        if (other.getOwner() != this) {
            CompatibilityHelper.injectorLoggerWarn("{} conflict. Skipping {} with priority {}, already redirected by {} with priority {}",
                    this.annotationType, this.info, this.meta.priority, other.name, other.priority);
            return false;
        }
        return true;
    }
}
