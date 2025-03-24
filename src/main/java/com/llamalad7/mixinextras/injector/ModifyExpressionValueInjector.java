package com.llamalad7.mixinextras.injector;

import com.llamalad7.mixinextras.expression.impl.flow.postprocessing.ArrayCreationInfo;
import com.llamalad7.mixinextras.expression.impl.flow.expansion.InsnExpander;
import com.llamalad7.mixinextras.expression.impl.utils.ComparisonInfo;
import com.llamalad7.mixinextras.expression.impl.utils.ExpressionDecorations;
import com.llamalad7.mixinextras.expression.impl.utils.FlowDecorations;
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
        node = InsnExpander.doExpansion(node, target, info);

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

        TargetInfo info = new TargetInfo(target, node);

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
        info.invokeHandler(valueType, after, stack);
        if (shouldPop) {
            after.add(new InsnNode(Opcodes.POP));
        }
        target.insns.insert(info.getInsertionPoint(valueNode), after);
    }

    private void invokeHandler(Type valueType, Target target, InsnList after, StackExtension stack) {
        InjectorData handler = new InjectorData(target, "expression value modifier");

        Type expectedDesc = IntLikeBehaviour.MatchReturnType.INSTANCE.transform(
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
        InjectorUtils.coerceReturnType(handler, after, valueType);
    }

    private Type getReturnType(InjectionNode node) {
        if (InjectorUtils.hasInjectorSpecificDecoration(node, info, ExpressionDecorations.IS_STRING_CONCAT_EXPRESSION)) {
            return Type.getType(String.class);
        }
        if (node.hasDecoration(ExpressionDecorations.SIMPLE_EXPRESSION_TYPE)) {
            return node.getDecoration(ExpressionDecorations.SIMPLE_EXPRESSION_TYPE);
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
        if (current.getOpcode() == Opcodes.NEW || current.getOpcode() == Opcodes.CHECKCAST) {
            TypeInsnNode typeInsnNode = (TypeInsnNode) current;
            return Type.getObjectType(typeInsnNode.desc);
        }

        if (current.getOpcode() == Opcodes.INSTANCEOF) {
            return Type.BOOLEAN_TYPE;
        }

        {
            Type constantType = ASMUtils.getConstantType(current);
            if (constantType != null) {
                return constantType;
            }
        }

        return null;
    }

    private class TargetInfo {
        private final Target target;
        private final boolean isDupedFactoryRedirect;
        private final boolean isDynamicInstanceofRedirect;
        private final ArrayCreationInfo arrayCreationInfo;
        private final boolean isStringConcat;
        private final ComparisonInfo comparison;

        public TargetInfo(Target target, InjectionNode node) {
            this.target = target;
            this.isDupedFactoryRedirect = InjectorUtils.isDupedFactoryRedirect(node);
            this.isDynamicInstanceofRedirect = InjectorUtils.isDynamicInstanceofRedirect(node);
            this.arrayCreationInfo = node.getDecoration(FlowDecorations.ARRAY_CREATION_INFO);
            this.isStringConcat = InjectorUtils.hasInjectorSpecificDecoration(node, info, ExpressionDecorations.IS_STRING_CONCAT_EXPRESSION);
            this.comparison = InjectorUtils.getInjectorSpecificDecoration(node, info, ExpressionDecorations.COMPARISON_INFO);
        }

        public AbstractInsnNode getInsertionPoint(AbstractInsnNode valueNode) {
            if (isDupedFactoryRedirect) {
                return PreviousInjectorInsns.DUPED_FACTORY_REDIRECT.getLast(valueNode);
            }
            if (isDynamicInstanceofRedirect) {
                return PreviousInjectorInsns.DYNAMIC_INSTANCEOF_REDIRECT.getLast(valueNode);
            }
            if (arrayCreationInfo != null) {
                return arrayCreationInfo.initialized.getNode(target).getCurrentTarget();
            }
            if (comparison != null) {
                return comparison.getJumpInsn(target);
            }
            return valueNode;
        }

        public void invokeHandler(Type valueType, InsnList after, StackExtension stack) {
            LabelNode originalJumpTarget = null;
            if (isStringConcat) {
                // We copy the StringBuilder, build it, let the user modify the String, and then replace the
                // contents of the StringBuilder with what they returned.
                after.add(new InsnNode(Opcodes.DUP));
                stack.extra(1);
                after.add(new MethodInsnNode(
                        Opcodes.INVOKEVIRTUAL,
                        Type.getInternalName(StringBuilder.class),
                        "toString",
                        Bytecode.generateDescriptor(String.class),
                        false
                ));
            } else if (comparison != null) {
                originalJumpTarget = comparison.getJumpTarget(target);
                ASMUtils.ifElse(
                        after,
                        label -> comparison.getJumpInsn(target).label = label,
                        () -> after.add(new InsnNode(comparison.jumpOnTrue ? Opcodes.ICONST_0 : Opcodes.ICONST_1)),
                        () -> after.add(new InsnNode(comparison.jumpOnTrue ? Opcodes.ICONST_1 : Opcodes.ICONST_0))
                );
            }
            ModifyExpressionValueInjector.this.invokeHandler(valueType, target, after, stack);
            if (isStringConcat) {
                after.add(
                        new MethodInsnNode(
                                Opcodes.INVOKESTATIC,
                                Type.getInternalName(MixinExtrasHooks.class),
                                "replaceContents",
                                Bytecode.generateDescriptor(StringBuilder.class, StringBuilder.class, String.class),
                                false
                        )
                );
            } else if (comparison != null) {
                after.add(new JumpInsnNode(comparison.jumpOnTrue ? Opcodes.IFNE : Opcodes.IFEQ, originalJumpTarget));
            }
        }
    }
}
