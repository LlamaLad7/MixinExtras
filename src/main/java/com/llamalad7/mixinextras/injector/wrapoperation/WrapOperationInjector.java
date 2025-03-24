package com.llamalad7.mixinextras.injector.wrapoperation;

import com.llamalad7.mixinextras.expression.impl.flow.expansion.InsnExpander;
import com.llamalad7.mixinextras.expression.impl.utils.ComparisonInfo;
import com.llamalad7.mixinextras.expression.impl.utils.ExpressionDecorations;
import com.llamalad7.mixinextras.injector.IntLikeBehaviour;
import com.llamalad7.mixinextras.injector.StackExtension;
import com.llamalad7.mixinextras.service.MixinExtrasService;
import com.llamalad7.mixinextras.utils.*;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes.InjectionNode;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.util.Bytecode;

import java.util.*;
import java.util.function.Consumer;

class WrapOperationInjector extends Injector {
    private final Type operationType = MixinExtrasService.getInstance().changePackage(Operation.class, Type.getType(CompatibilityHelper.getAnnotation(info).desc), WrapOperation.class);
    private final List<OperationConstructor> operationTypes = Arrays.asList(
            DynamicInstanceofRedirectOperation::new,
            DupedFactoryRedirectOperation::new,
            this::newComparisonExpression,
            MethodCallOperation::new,
            FieldAccessOperation::new,
            InstanceofOperation::new,
            this::newInstantiationOperation,
            SimpleOperation::new
    );

    public WrapOperationInjector(InjectionInfo info) {
        super(info, "@WrapOperation");
    }

    @Override
    protected void inject(Target target, InjectionNode initialNode) {
        InjectionNode node = InsnExpander.doExpansion(initialNode, target, info);

        this.checkTargetModifiers(target, false);
        StackExtension stack = new StackExtension(target);
        OperationType operation = operationTypes.stream()
                .map(it -> it.make(target, node, stack))
                .filter(Objects::nonNull)
                .filter(OperationType::validate)
                .findFirst()
                .orElseThrow(
                        () -> CompatibilityHelper.makeInvalidInjectionException(
                                this.info,
                                String.format(
                                        "%s annotation is targeting an invalid insn in %s in %s",
                                        this.annotationType, target, this
                                )
                        )
                );
        this.wrapOperation(target, operation, stack);
    }

    private void wrapOperation(Target target, OperationType operation, StackExtension stack) {
        InsnList insns = new InsnList();
        InjectionNode node = operation.node;
        Type[] argTypes = getCurrentArgTypes(node);
        Type returnType = getReturnType(node);

        AbstractInsnNode champion = this.invokeHandler(target, operation, node, argTypes, returnType, insns, stack);
        operation.afterHandlerCall(insns, champion);

        AbstractInsnNode finalTarget = node.getCurrentTarget();
        target.wrapNode(finalTarget, champion, insns, new InsnList());
        node.decorate(Decorations.WRAPPED, true);
        target.insns.remove(finalTarget);
    }

    private AbstractInsnNode invokeHandler(Target target, OperationType operation, InjectionNode node, Type[] argTypes, Type returnType, InsnList insns, StackExtension stack) {
        InjectorData handler = new InjectorData(target, "operation wrapper");
        boolean hasExtraThis = node.isReplaced() && node.getCurrentTarget().getOpcode() != Opcodes.INVOKESTATIC;
        if (hasExtraThis) {
            // We will add the extra `this` in ourselves in the generated bridge method later.
            argTypes = ArrayUtils.remove(argTypes, 0);
        }
        Type[] originalArgs = getOriginalArgTypes(node);
        this.validateParams(handler, returnType, ArrayUtils.add(originalArgs, operationType));

        // Store *all* the args, including ones added by redirectors and previous operation wrappers.
        // Excess ones will be bound to the lambda.
        int[] argMap = this.storeArgs(target, argTypes, insns, 0);
        if (hasExtraThis) {
            insns.add(new InsnNode(Opcodes.POP));
        }

        if (!this.isStatic) {
            insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
        }
        // Push the args which should go to the handler method.
        this.pushArgs(this.methodArgs, insns, argMap, 0, originalArgs.length);
        // Push the receiver of the bridge method (if applicable) and any captured parameters it will need.
        if (hasExtraThis) {
            insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
        }
        this.pushArgs(argTypes, insns, argMap, originalArgs.length, argMap.length);
        // The trailing params are any arguments which come after the original args, and should therefore be bound to the lambda.
        this.makeOperation(operation, originalArgs, returnType, insns, hasExtraThis, ArrayUtils.subarray(argTypes, originalArgs.length, argTypes.length));
        if (handler.captureTargetArgs > 0) {
            this.pushArgs(target.arguments, insns, target.getArgIndices(), 0, handler.captureTargetArgs);
        }

        stack.receiver(this.isStatic);
        stack.extra(1); // Operation
        stack.capturedArgs(target.arguments, handler.captureTargetArgs);

        AbstractInsnNode result = super.invokeHandler(insns);
        InjectorUtils.coerceReturnType(handler, insns, returnType);
        return result;
    }

    private void makeOperation(OperationType operation, Type[] argTypes, Type returnType, InsnList insns, boolean hasExtraThis, Type[] trailingParams) {
        OperationUtils.makeOperation(
                argTypes, returnType, insns, hasExtraThis, trailingParams, classNode, operationType, operation.getName(),
                (paramArrayIndex, loadArgs) -> {
                    InsnList copied = new InsnList();
                    operation.copyNode(copied, paramArrayIndex, loadArgs);
                    return copied;
                }
        );
    }

    private Type getReturnType(InjectionNode node) {
        AbstractInsnNode originalTarget = node.getOriginalTarget();
        AbstractInsnNode currentTarget = node.getCurrentTarget();

        if (node.hasDecoration(ExpressionDecorations.SIMPLE_OPERATION_RETURN_TYPE)) {
            return node.getDecoration(ExpressionDecorations.SIMPLE_OPERATION_RETURN_TYPE);
        }

        if (originalTarget.getOpcode() == Opcodes.INSTANCEOF) {
            return Type.BOOLEAN_TYPE;
        }

        if (currentTarget instanceof MethodInsnNode) {
            MethodInsnNode methodInsnNode = (MethodInsnNode) currentTarget;
            if (methodInsnNode.name.equals("<init>")) {
                return Type.getObjectType(methodInsnNode.owner);
            }
            return Type.getReturnType(methodInsnNode.desc);
        }
        if (currentTarget instanceof FieldInsnNode) {
            FieldInsnNode fieldInsnNode = (FieldInsnNode) currentTarget;
            if (fieldInsnNode.getOpcode() == Opcodes.GETFIELD || fieldInsnNode.getOpcode() == Opcodes.GETSTATIC) {
                return Type.getType(fieldInsnNode.desc);
            }
            return Type.VOID_TYPE;
        }

        throw new UnsupportedOperationException();
    }

    private Type[] getOriginalArgTypes(InjectionNode node) {
        if (node.hasDecoration(Decorations.NEW_ARG_TYPES)) {
            return node.getDecoration(Decorations.NEW_ARG_TYPES);
        }
        if (node.hasDecoration(ExpressionDecorations.SIMPLE_OPERATION_ARGS)) {
            return cleanIntLikeArgs(node.getDecoration(ExpressionDecorations.SIMPLE_OPERATION_ARGS));
        }
        return getEffectiveArgTypes(node.getOriginalTarget());
    }

    private Type[] getCurrentArgTypes(InjectionNode node) {
        if (!node.isReplaced() && node.hasDecoration(ExpressionDecorations.SIMPLE_OPERATION_ARGS)) {
            return cleanIntLikeArgs(node.getDecoration(ExpressionDecorations.SIMPLE_OPERATION_ARGS));
        }
        return getEffectiveArgTypes(node.getCurrentTarget());
    }

    private Type[] cleanIntLikeArgs(Type[] originalArgs) {
        return new IntLikeBehaviour.MatchArgType(0).transform(
                info,
                Type.getMethodType(returnType, originalArgs),
                Type.getMethodType(this.returnType, methodArgs)
        ).getArgumentTypes();
    }

    private Type[] getEffectiveArgTypes(AbstractInsnNode node) {
        if (node instanceof MethodInsnNode) {
            MethodInsnNode methodInsnNode = ((MethodInsnNode) node);
            Type[] args = Type.getArgumentTypes(methodInsnNode.desc);
            if (methodInsnNode.name.equals("<init>")) {
                // The receiver isn't truly an arg because we'll make it ourselves.
                return args;
            }
            switch (methodInsnNode.getOpcode()) {
                case Opcodes.INVOKESTATIC:
                    break;
                case Opcodes.INVOKESPECIAL:
                    args = ArrayUtils.add(args, 0, Type.getObjectType(classNode.name));
                    break;
                default:
                    args = ArrayUtils.add(args, 0, Type.getObjectType(methodInsnNode.owner));
            }
            return args;
        }
        if (node instanceof FieldInsnNode) {
            FieldInsnNode fieldInsnNode = ((FieldInsnNode) node);
            switch (fieldInsnNode.getOpcode()) {
                case Opcodes.GETFIELD:
                    return new Type[]{Type.getObjectType(fieldInsnNode.owner)};
                case Opcodes.PUTFIELD:
                    return new Type[]{Type.getObjectType(fieldInsnNode.owner), Type.getType(fieldInsnNode.desc)};
                case Opcodes.GETSTATIC:
                    return new Type[0];
                case Opcodes.PUTSTATIC:
                    return new Type[]{Type.getType(fieldInsnNode.desc)};
            }
        }
        if (node.getOpcode() == Opcodes.INSTANCEOF) {
            return new Type[]{ASMUtils.OBJECT_TYPE};
        }

        throw new UnsupportedOperationException();
    }

    private abstract static class OperationType {
        protected final Target target;
        protected final InjectionNode node;
        protected final AbstractInsnNode originalTarget;
        protected final AbstractInsnNode currentTarget;
        protected final StackExtension stack;

        OperationType(Target target, InjectionNode node, StackExtension stack) {
            this.target = target;
            this.node = node;
            this.originalTarget = node.getOriginalTarget();
            this.currentTarget = node.getCurrentTarget();
            this.stack = stack;
        }

        abstract boolean validate();

        abstract String getName();

        void copyNode(InsnList insns, int paramArrayIndex, Consumer<InsnList> loadArgs) {
            loadArgs.accept(insns);
            insns.add(currentTarget.clone(Collections.emptyMap()));
        }

        void afterHandlerCall(InsnList insns, AbstractInsnNode champion) {
        }
    }

    private class MethodCallOperation extends OperationType {
        MethodCallOperation(Target target, InjectionNode node, StackExtension stack) {
            super(target, node, stack);
        }

        @Override
        boolean validate() {
            if (currentTarget instanceof MethodInsnNode) {
                MethodInsnNode methodInsnNode = (MethodInsnNode) currentTarget;
                if (methodInsnNode.name.equals("<init>")) {
                    throw CompatibilityHelper.makeInvalidInjectionException(
                            info,
                            String.format(
                                    "%s annotation is trying to target an <init> call in %s in %s! If this is an instantiation, target the NEW instead.",
                                    annotationType, target, WrapOperationInjector.this
                            )
                    );
                }
                return true;
            }
            return false;
        }

        @Override
        String getName() {
            return ((MethodInsnNode) currentTarget).name;
        }
    }

    private static class FieldAccessOperation extends OperationType {
        FieldAccessOperation(Target target, InjectionNode node, StackExtension stack) {
            super(target, node, stack);
        }

        @Override
        boolean validate() {
            return currentTarget instanceof FieldInsnNode;
        }

        @Override
        String getName() {
            return ((FieldInsnNode) currentTarget).name;
        }
    }

    private static class InstanceofOperation extends OperationType {
        InstanceofOperation(Target target, InjectionNode node, StackExtension stack) {
            super(target, node, stack);
        }

        @Override
        boolean validate() {
            return currentTarget.getOpcode() == Opcodes.INSTANCEOF;
        }

        @Override
        String getName() {
            return "instanceof" + StringUtils.substringAfterLast(((TypeInsnNode) currentTarget).desc, "/");
        }
    }

    private static class InstantiationOperation extends OperationType {
        private final InjectionNode newNode;
        private final AbstractInsnNode newInsn;
        private final boolean isDuped;

        InstantiationOperation(Target target, InjectionNode node, StackExtension stack, InjectionNode newNode) {
            super(target, node, stack);
            this.newNode = newNode;
            this.newInsn = newNode.getCurrentTarget();
            this.isDuped = InjectorUtils.isDupedNew(newNode);
        }

        @Override
        boolean validate() {
            return true;
        }

        @Override
        String getName() {
            return "new" + StringUtils.substringAfterLast(((MethodInsnNode) currentTarget).owner, "/");
        }

        @Override
        void copyNode(InsnList insns, int paramArrayIndex, Consumer<InsnList> loadArgs) {
            insns.add(new TypeInsnNode(Opcodes.NEW, ((MethodInsnNode) currentTarget).owner));
            insns.add(new InsnNode(Opcodes.DUP));
            super.copyNode(insns, paramArrayIndex, loadArgs);
        }

        @Override
        void afterHandlerCall(InsnList insns, AbstractInsnNode champion) {
            AbstractInsnNode newReplacement;
            if (isDuped) {
                // We replace the `NEW` object with a `null` reference, for convenience:
                newReplacement = new InsnNode(Opcodes.ACONST_NULL);
                // Then, after invoking the handler, we have 2 null references and the actual new object on the stack.
                // We want to get rid of the null references:
                stack.extra(1);
                insns.add(new InsnNode(Opcodes.DUP_X2));
                insns.add(new InsnNode(Opcodes.POP));
                insns.add(new InsnNode(Opcodes.POP));
                insns.add(new InsnNode(Opcodes.POP));
            } else {
                // "Get rid" of the `NEW` instruction:
                newReplacement = new InsnNode(Opcodes.NOP);
                // And pop the result of the wrapper since it isn't used:
                insns.add(new InsnNode(Opcodes.POP));
            }
            newNode.replace(champion);
            // We've already replaced the <init> call, but we want to replace the NEW instruction.
            target.insns.set(newInsn, newReplacement);
        }
    }

    private OperationType newInstantiationOperation(Target target, InjectionNode node, StackExtension stack) {
        AbstractInsnNode newNode = node.getCurrentTarget();
        if (newNode.getOpcode() != Opcodes.NEW) {
            return null;
        }
        node.decorate(Decorations.WRAPPED, true);
        return new InstantiationOperation(
                target,
                target.addInjectionNode(ASMUtils.findInitNodeFor(target, (TypeInsnNode) newNode)),
                stack,
                node
        );
    }

    private class DynamicInstanceofRedirectOperation extends MethodCallOperation {
        DynamicInstanceofRedirectOperation(Target target, InjectionNode node, StackExtension stack) {
            super(target, node, stack);
        }

        @Override
        boolean validate() {
            return super.validate() && InjectorUtils.isDynamicInstanceofRedirect(node);
        }

        @Override
        void copyNode(InsnList insns, int paramArrayIndex, Consumer<InsnList> loadArgs) {
            super.copyNode(insns, paramArrayIndex, loadArgs);
            // We have a Class object and need to get it back to a boolean using the first element of the lambda args.
            // The code added by RedirectInjector expects a reference to the checked object already on the stack, so we
            // load the first and only lambda arg, and then swap it with the Class<?> result from the redirector.
            insns.add(new VarInsnNode(Opcodes.ALOAD, paramArrayIndex));
            insns.add(new InsnNode(Opcodes.ICONST_0));
            insns.add(new InsnNode(Opcodes.AALOAD));
            insns.add(new InsnNode(Opcodes.SWAP));
            // We need to encompass all the extra logic added by RedirectInjector
            PreviousInjectorInsns.DYNAMIC_INSTANCEOF_REDIRECT.moveNodes(target.insns, insns, currentTarget);
        }

        @Override
        void afterHandlerCall(InsnList insns, AbstractInsnNode champion) {
            // At this point, we have the boolean result and the checked object on the stack.
            // The object was DUPed by RedirectInjector before the handler was called, so removing the DUP is too risky.
            // Instead, we simply pop the excess object.
            insns.add(new InsnNode(Opcodes.SWAP));
            insns.add(new InsnNode(Opcodes.POP));
        }
    }

    private class DupedFactoryRedirectOperation extends MethodCallOperation {
        DupedFactoryRedirectOperation(Target target, InjectionNode node, StackExtension stack) {
            super(target, node, stack);
        }

        @Override
        boolean validate() {
            return super.validate() && InjectorUtils.isDupedFactoryRedirect(node);
        }

        @Override
        void copyNode(InsnList insns, int paramArrayIndex, Consumer<InsnList> loadArgs) {
            super.copyNode(insns, paramArrayIndex, loadArgs);
            // We need to encompass the null check added by RedirectInjector.
            PreviousInjectorInsns.DUPED_FACTORY_REDIRECT.moveNodes(target.insns, insns, currentTarget);
        }
    }

    private class ComparisonOperation extends MethodCallOperation {
        private final boolean isWrapped;
        private final ComparisonInfo comparison;

        ComparisonOperation(Target target, InjectionNode node, StackExtension stack, boolean isWrapped, ComparisonInfo comparison) {
            super(target, node, stack);
            this.isWrapped = isWrapped;
            this.comparison = comparison;
        }

        @Override
        boolean validate() {
            super.validate();
            return comparison != null;
        }

        @Override
        String getName() {
            return isWrapped ? super.getName() : "comparison";
        }

        @Override
        void copyNode(InsnList insns, int paramArrayIndex, Consumer<InsnList> loadArgs) {
            if (isWrapped) {
                super.copyNode(insns, paramArrayIndex, loadArgs);
                // Encompass the extra branching we added ourselves
                PreviousInjectorInsns.COMPARISON_WRAPPER.moveNodes(target.insns, insns, currentTarget);
                if (!comparison.jumpOnTrue) {
                    ASMUtils.ifElse(
                            insns,
                            Opcodes.IFNE,
                            () -> insns.add(new InsnNode(Opcodes.ICONST_1)),
                            () -> insns.add(new InsnNode(Opcodes.ICONST_0))
                    );
                }
                return;
            }
            loadArgs.accept(insns);
            ASMUtils.ifElse(
                    insns,
                    comparison.copyJump(insns),
                    () -> insns.add(new InsnNode(comparison.jumpOnTrue ? Opcodes.ICONST_0 : Opcodes.ICONST_1)),
                    () -> insns.add(new InsnNode(comparison.jumpOnTrue ? Opcodes.ICONST_1 : Opcodes.ICONST_0))
            );
        }

        @Override
        void afterHandlerCall(InsnList insns, AbstractInsnNode champion) {
            ASMUtils.ifElse(
                    insns,
                    Opcodes.IFNE,
                    () -> insns.add(new InsnNode(comparison.jumpOnTrue ? Opcodes.ICONST_0 : Opcodes.ICONST_1)),
                    () -> insns.add(new InsnNode(comparison.jumpOnTrue ? Opcodes.ICONST_1 : Opcodes.ICONST_0))
            );
            if (!isWrapped) {
                insns.add(new JumpInsnNode(Opcodes.IFNE, comparison.getJumpTarget(target)));
                comparison.cleanup(target);
            }
        }
    }

    private static class SimpleOperation extends OperationType {
        SimpleOperation(Target target, InjectionNode node, StackExtension stack) {
            super(target, node, stack);
        }

        @Override
        boolean validate() {
            return !node.isReplaced() &&
                    node.hasDecoration(ExpressionDecorations.SIMPLE_OPERATION_ARGS) &&
                    node.hasDecoration(ExpressionDecorations.SIMPLE_OPERATION_RETURN_TYPE);
        }

        @Override
        String getName() {
            return Bytecode.getOpcodeName(currentTarget).toLowerCase(Locale.ROOT);
        }
    }

    private OperationType newComparisonExpression(Target target, InjectionNode node, StackExtension stack) {
        ComparisonInfo comparison = InjectorUtils.getInjectorSpecificDecoration(node, info, ExpressionDecorations.COMPARISON_INFO);
        if (comparison == null) {
            return null;
        }
        boolean isWrapped = node.hasDecoration(Decorations.WRAPPED);
        return new ComparisonOperation(
                target,
                node,
                stack,
                isWrapped,
                comparison
        );
    }

    @FunctionalInterface
    private interface OperationConstructor {
        OperationType make(Target target, InjectionNode node, StackExtension stack);
    }
}
