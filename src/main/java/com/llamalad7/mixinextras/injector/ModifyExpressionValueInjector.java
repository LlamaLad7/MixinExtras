package com.llamalad7.mixinextras.injector;

import com.llamalad7.mixinextras.utils.ASMUtils;
import com.llamalad7.mixinextras.utils.CompatibilityHelper;
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

        AbstractInsnNode valueNode = node.getCurrentTarget();
        Type valueType = getReturnType(valueNode);
        if (valueNode instanceof TypeInsnNode && valueNode.getOpcode() == Opcodes.NEW) {
            valueNode = ASMUtils.findInitNodeFor(target, (TypeInsnNode) valueNode);
        }

        this.injectValueModifier(target, valueNode, valueType);
    }

    private void checkTargetReturnsAValue(Target target, InjectionNode node) {
        Type returnType = getReturnType(node.getCurrentTarget());
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

    private void injectValueModifier(Target target, AbstractInsnNode valueNode, Type valueType) {
        Target.Extension extraStack = target.extendStack();
        final InsnList after = new InsnList();
        this.invokeHandler(valueType, target, extraStack, after);
        extraStack.apply();
        target.insns.insert(valueNode, after);
    }

    private void invokeHandler(Type valueType, Target target, Target.Extension extraStack, InsnList after) {
        InjectorData handler = new InjectorData(target, "expression value modifier");
        this.validateParams(handler, valueType, valueType);

        if (!this.isStatic) {
            after.add(new VarInsnNode(Opcodes.ALOAD, 0));
            if (valueType.getSize() == 2) {
                after.add(new InsnNode(Opcodes.DUP_X2));
                after.add(new InsnNode(Opcodes.POP));
            } else {
                after.add(new InsnNode(Opcodes.SWAP));
            }
        }

        if (handler.captureTargetArgs > 0) {
            this.pushArgs(target.arguments, after, target.getArgIndices(), 0, handler.captureTargetArgs, extraStack);
        }

        this.invokeHandler(after);
    }

    private Type getReturnType(AbstractInsnNode node) {
        if (node instanceof MethodInsnNode) {
            MethodInsnNode methodInsnNode = (MethodInsnNode) node;
            return Type.getReturnType(methodInsnNode.desc);
        }

        if (node instanceof FieldInsnNode) {
            FieldInsnNode fieldInsnNode = (FieldInsnNode) node;
            if (fieldInsnNode.getOpcode() == Opcodes.GETFIELD || fieldInsnNode.getOpcode() == Opcodes.GETSTATIC) {
                return Type.getType(fieldInsnNode.desc);
            }
            return Type.VOID_TYPE;
        }

        if (Bytecode.isConstant(node)) {
            return Bytecode.getConstantType(node);
        }

        if (node instanceof TypeInsnNode && node.getOpcode() == Opcodes.NEW) {
            TypeInsnNode typeInsnNode = ((TypeInsnNode) node);
            return Type.getObjectType(typeInsnNode.desc);
        }

        return null;
    }
}
