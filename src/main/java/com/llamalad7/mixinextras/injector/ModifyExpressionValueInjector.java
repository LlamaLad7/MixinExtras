package com.llamalad7.mixinextras.injector;

import com.llamalad7.mixinextras.utils.*;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes.InjectionNode;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.util.Bytecode;

public class ModifyExpressionValueInjector extends Injector {

    public ModifyExpressionValueInjector(InjectionInfo info) {
        super(info, "@ModifyExpressionValue");
    }

    @Override
    protected void inject(Target target, InjectionNode node) {
        this.checkTargetReturnsAValue(target, node);
        this.checkTargetModifiers(target, false);

        StackExtension stack = new StackExtension(target);
        AbstractInsnNode valueNode = node.getCurrentTarget();
        Type valueType = getReturnType(node);

        boolean shouldPop = false;
        if (valueNode instanceof TypeInsnNode && valueNode.getOpcode() == Opcodes.NEW) {
            if (!InjectorUtils.isDupedNew(node)) {
                target.insns.insert(valueNode, new InsnNode(Opcodes.DUP));
                stack.extra(1);
                node.decorate(Decorations.NEW_IS_DUPED, true);
                shouldPop = true;
            }
            valueNode = ASMUtils.findInitNodeFor(target, (TypeInsnNode) valueNode);
        }

        TargetInfo info = new TargetInfo(node);

        this.injectValueModifier(target, valueNode, valueType, info, shouldPop, stack);
    }

    private void checkTargetReturnsAValue(Target target, InjectionNode node) {
        Type returnType = getReturnType(node);
        if (returnType == Type.VOID_TYPE) {
            throw CompatibilityHelper.makeInvalidInjectionException(this.info,
                    String.format(
                            "%s annotation is targeting an instruction with a return type of 'void' in %s in %s",
                            this.annotationType, target, this
                    ));
        }

        if (returnType == null) {
            throw CompatibilityHelper.makeInvalidInjectionException(this.info, String.format("%s annotation is targeting an invalid insn in %s in %s",
                    this.annotationType, target, this));
        }
    }

    private void injectValueModifier(Target target, AbstractInsnNode valueNode, Type valueType, TargetInfo info, boolean shouldPop, StackExtension stack) {
        final InsnList after = new InsnList();
        this.invokeHandler(valueType, target, after, stack);
        if (shouldPop) {
            after.add(new InsnNode(Opcodes.POP));
        }
        target.insns.insert(info.getInsertionPoint(valueNode), after);
    }

    private void invokeHandler(Type valueType, Target target, InsnList after, StackExtension stack) {
        InjectorData handler = new InjectorData(target, "expression value modifier");

        Type expectedDesc = IntLikeBehaviour.MatchReturnType.INSTANCE.handle(
                info,
                Type.getMethodType(valueType, valueType),
                Type.getMethodType(returnType, methodArgs)
        );
        this.validateParams(handler, expectedDesc.getReturnType(), expectedDesc.getArgumentTypes());

        if (!this.isStatic) {
            after.add(new VarInsnNode(Opcodes.ALOAD, 0));
            if (valueType.getSize() == 2) {
                stack.extra(1);
                after.add(new InsnNode(Opcodes.DUP_X2));
                after.add(new InsnNode(Opcodes.POP));
            } else {
                after.add(new InsnNode(Opcodes.SWAP));
            }
        }

        if (handler.captureTargetArgs > 0) {
            this.pushArgs(target.arguments, after, target.getArgIndices(), 0, handler.captureTargetArgs);
        }

        stack.receiver(this.isStatic);
        stack.capturedArgs(target.arguments, handler.captureTargetArgs);

        this.invokeHandler(after);
    }

    private Type getReturnType(InjectionNode node) {
        if (node.hasDecoration(Decorations.SIMPLE_EXPRESSION_TYPE)) {
            return node.getDecoration(Decorations.SIMPLE_EXPRESSION_TYPE);
        }
        AbstractInsnNode current = node.getCurrentTarget();

        if (current instanceof MethodInsnNode) {
            MethodInsnNode methodInsnNode = (MethodInsnNode) current;
            return Type.getReturnType(methodInsnNode.desc);
        }

        if (current instanceof FieldInsnNode) {
            FieldInsnNode fieldInsnNode = (FieldInsnNode) current;
            if (fieldInsnNode.getOpcode() == Opcodes.GETFIELD || fieldInsnNode.getOpcode() == Opcodes.GETSTATIC) {
                return Type.getType(fieldInsnNode.desc);
            }
            return Type.VOID_TYPE;
        }

        if (Bytecode.isConstant(current)) {
            return Bytecode.getConstantType(current);
        }

        if (current instanceof TypeInsnNode && current.getOpcode() == Opcodes.NEW) {
            TypeInsnNode typeInsnNode = ((TypeInsnNode) current);
            return Type.getObjectType(typeInsnNode.desc);
        }

        return null;
    }

    private static class TargetInfo {
        public final boolean isDupedFactoryRedirect;
        public final boolean isDynamicInstanceofRedirect;

        public TargetInfo(InjectionNode node) {
            this.isDupedFactoryRedirect = InjectorUtils.isDupedFactoryRedirect(node);
            this.isDynamicInstanceofRedirect = InjectorUtils.isDynamicInstanceofRedirect(node);
        }

        public AbstractInsnNode getInsertionPoint(AbstractInsnNode valueNode) {
            if (isDupedFactoryRedirect) {
                return PreviousInjectorInsns.DUPED_FACTORY_REDIRECT.getLast(valueNode);
            }
            if (isDynamicInstanceofRedirect) {
                return PreviousInjectorInsns.DYNAMIC_INSTANCEOF_REDIRECT.getLast(valueNode);
            }
            return valueNode;
        }
    }
}
