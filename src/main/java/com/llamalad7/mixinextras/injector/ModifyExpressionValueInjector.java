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

import java.util.function.Supplier;

public class ModifyExpressionValueInjector extends Injector {
    private static final MixinExtrasLogger LOGGER = MixinExtrasLogger.get("ModifyExpressionValue");

    public ModifyExpressionValueInjector(InjectionInfo info) {
        super(info, "@ModifyExpressionValue");
    }

    @Override
    protected void inject(Target target, InjectionNode node) {
        this.checkTargetReturnsAValue(target, node);
        this.checkTargetModifiers(target, false);

        AbstractInsnNode valueNode = node.getCurrentTarget();
        Type valueType = getReturnType(valueNode);
        boolean shouldPop = false;
        if (valueNode instanceof TypeInsnNode && valueNode.getOpcode() == Opcodes.NEW) {
            if (!InjectorUtils.isDupedNew(node)) {
                target.insns.insert(valueNode, new InsnNode(Opcodes.DUP));
                node.decorate(Decorations.NEW_IS_DUPED, true);
                shouldPop = true;
            }
            valueNode = ASMUtils.findInitNodeFor(target, (TypeInsnNode) valueNode);
        }

        this.injectValueModifier(target, valueNode, valueType, InjectorUtils.isDupedFactoryRedirect(node), shouldPop);
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

    private void injectValueModifier(Target target, AbstractInsnNode valueNode, Type valueType, boolean isDupedFactoryRedirect, boolean shouldPop) {
        Target.Extension extraStack = target.extendStack();
        final InsnList after = new InsnList();
        this.invokeHandler(valueType, target, extraStack, after);
        extraStack.apply();
        if (shouldPop) {
            after.add(new InsnNode(Opcodes.POP));
        }
        target.insns.insert(getInsertionPoint(valueNode, target, isDupedFactoryRedirect), after);
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

    private AbstractInsnNode getInsertionPoint(AbstractInsnNode valueNode, Target target, boolean isDupedFactoryRedirect) {
        if (!isDupedFactoryRedirect) {
            return valueNode;
        }
        AbstractInsnNode node = InjectorUtils.findFactoryRedirectThrowString(target, valueNode);
        if (node == null) {
            return valueNode;
        }
        String message = ((String) ((LdcInsnNode) node).cst);
        Supplier<AbstractInsnNode> failed = () -> {
            LOGGER.warn(
                    "Please inform LlamaLad7! Failed to find end of factory redirect throw for '{}'",
                    message
            );
            return valueNode;
        };
        if ((node = node.getNext()).getOpcode() != Opcodes.INVOKESPECIAL) return failed.get();
        if ((node = node.getNext()).getOpcode() != Opcodes.ATHROW) return failed.get();
        if (!((node = node.getNext()) instanceof LabelNode)) return failed.get();
        return node;
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
