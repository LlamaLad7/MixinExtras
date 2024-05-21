package com.llamalad7.mixinextras.injector.wrapoperation;

import com.llamalad7.mixinextras.injector.StackExtension;
import com.llamalad7.mixinextras.service.MixinExtrasService;
import com.llamalad7.mixinextras.utils.*;
import org.apache.commons.lang3.ArrayUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes.InjectionNode;
import org.spongepowered.asm.mixin.injection.struct.Target;

import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.Predicate;

class WrapOperationInjector extends Injector {
    private static final String NPE = Type.getInternalName(NullPointerException.class);

    private final Type operationType = MixinExtrasService.getInstance().changePackage(Operation.class, Type.getType(CompatibilityHelper.getAnnotation(info).desc), WrapOperation.class);

    public WrapOperationInjector(InjectionInfo info) {
        super(info, "@WrapOperation");
    }

    @Override
    protected void inject(Target target, InjectionNode node) {
        this.checkTargetModifiers(target, false);
        this.checkNode(target, node);
        this.wrapOperation(target, node);
    }

    private void checkNode(Target target, InjectionNode node) {
        AbstractInsnNode originalTarget = node.getOriginalTarget();
        AbstractInsnNode currentTarget = node.getCurrentTarget();
        if (currentTarget instanceof MethodInsnNode) {
            MethodInsnNode methodInsnNode = ((MethodInsnNode) currentTarget);
            if (methodInsnNode.name.equals("<init>")) {
                throw CompatibilityHelper.makeInvalidInjectionException(
                        this.info,
                        String.format(
                                "%s annotation is trying to target an <init> call in %s in %s! If this is an instantiation, target the NEW instead.",
                                this.annotationType, target, this
                        )
                );
            }
            return;
        }
        if (!(currentTarget instanceof FieldInsnNode ||
                originalTarget.getOpcode() == Opcodes.INSTANCEOF ||
                originalTarget.getOpcode() == Opcodes.NEW)) {
            throw CompatibilityHelper.makeInvalidInjectionException(
                    this.info,
                    String.format(
                            "%s annotation is targeting an invalid insn in %s in %s",
                            this.annotationType, target, this
                    )
            );
        }
    }

    private void wrapOperation(Target target, InjectionNode node) {
        StackExtension stack = new StackExtension(target);
        AbstractInsnNode initialTarget = node.getCurrentTarget();
        InsnList insns = new InsnList();
        boolean isNew = initialTarget.getOpcode() == Opcodes.NEW;
        boolean isDupedNew = InjectorUtils.isDupedNew(node);
        if (isNew) {
            node.decorate(Decorations.WRAPPED, true);
            node = target.addInjectionNode(ASMUtils.findInitNodeFor(target, (TypeInsnNode) initialTarget));
        }
        Type[] argTypes = getCurrentArgTypes(node);
        Type returnType = getReturnType(node);
        AbstractInsnNode champion = this.invokeHandler(target, node, argTypes, returnType, insns, stack);
        if (isDupedNew) {
            // We replace the `NEW` object with a `null` reference, for convenience:
            target.insns.set(initialTarget, new InsnNode(Opcodes.ACONST_NULL));
            // Then, after invoking the handler, we have 2 null references and the actual new object on the stack.
            // We want to get rid of the null references:
            stack.extra(1);
            insns.add(new InsnNode(Opcodes.DUP_X2));
            insns.add(new InsnNode(Opcodes.POP));
            insns.add(new InsnNode(Opcodes.POP));
            insns.add(new InsnNode(Opcodes.POP));
        } else if (isNew) {
            // "Get rid" of the `NEW` instruction:
            target.insns.set(initialTarget, new InsnNode(Opcodes.NOP));
            // And pop the result of the wrapper since it isn't used:
            insns.add(new InsnNode(Opcodes.POP));
        }
        AbstractInsnNode finalTarget = node.getCurrentTarget();
        target.wrapNode(finalTarget, champion, insns, new InsnList());
        if (isNew) {
            // We've already replaced the <init> call, but we want to replace the NEW instruction.
            target.getInjectionNode(initialTarget).replace(champion);
        }
        node.decorate(Decorations.WRAPPED, true);
        target.insns.remove(finalTarget);
    }

    private AbstractInsnNode invokeHandler(Target target, InjectionNode node, Type[] argTypes, Type returnType, InsnList insns, StackExtension stack) {
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
        this.makeOperation(target, originalArgs, returnType, node, insns, hasExtraThis, ArrayUtils.subarray(argTypes, originalArgs.length, argTypes.length));
        if (handler.captureTargetArgs > 0) {
            this.pushArgs(target.arguments, insns, target.getArgIndices(), 0, handler.captureTargetArgs);
        }

        stack.receiver(this.isStatic);
        stack.extra(1); // Operation
        stack.capturedArgs(target.arguments, handler.captureTargetArgs);

        AbstractInsnNode champion = super.invokeHandler(insns);

        if (InjectorUtils.isDynamicInstanceofRedirect(node)) {
            // At this point, we have the boolean result and the checked object on the stack.
            // The object was DUPed by RedirectInjector before the handler was called, so removing the DUP is too risky.
            // Instead, we simply pop the excess object.
            insns.add(new InsnNode(Opcodes.SWAP));
            insns.add(new InsnNode(Opcodes.POP));
        }
        return champion;
    }

    private void makeOperation(Target target, Type[] argTypes, Type returnType, InjectionNode node, InsnList insns, boolean hasExtraThis, Type[] trailingParams) {
        OperationUtils.makeOperation(
                argTypes, returnType, insns, hasExtraThis, trailingParams, classNode, operationType, getName(node.getCurrentTarget()),
                (paramArrayIndex, loadArgs) -> copyNode(node, paramArrayIndex, target, loadArgs)
        );
    }

    private InsnList copyNode(InjectionNode node, int paramArrayIndex, Target target, Consumer<InsnList> loadArgs) {
        AbstractInsnNode currentTarget = node.getCurrentTarget();
        InsnList insns = new InsnList();
        if (currentTarget instanceof MethodInsnNode) {
            MethodInsnNode methodInsnNode = (MethodInsnNode) currentTarget;
            if (methodInsnNode.name.equals("<init>")) {
                insns.add(new TypeInsnNode(Opcodes.NEW, methodInsnNode.owner));
                insns.add(new InsnNode(Opcodes.DUP));
            }
        }

        loadArgs.accept(insns);
        insns.add(currentTarget.clone(Collections.emptyMap()));

        if (InjectorUtils.isDynamicInstanceofRedirect(node)) {
            // We have a Class object and need to get it back to a boolean using the first element of the lambda args.
            // The code added by RedirectInjector expects a reference to the checked object already on the stack, so we
            // load the first and only lambda arg, and then swap it with the Class<?> result from the redirector.
            insns.add(new VarInsnNode(Opcodes.ALOAD, paramArrayIndex));
            insns.add(new InsnNode(Opcodes.ICONST_0));
            insns.add(new InsnNode(Opcodes.AALOAD));
            insns.add(new InsnNode(Opcodes.SWAP));
            // Sanity checks for the instructions being moved based on what I expect RedirectInjector to have added.
            checkAndMoveNodes(
                    target.insns,
                    insns,
                    currentTarget,
                    it -> it.getOpcode() == Opcodes.DUP,
                    it -> it.getOpcode() == Opcodes.IFNONNULL,
                    it -> it.getOpcode() == Opcodes.NEW && ((TypeInsnNode) it).desc.equals(NPE),
                    it -> it.getOpcode() == Opcodes.DUP,
                    it -> it instanceof LdcInsnNode && ((LdcInsnNode) it).cst instanceof String,
                    it -> it.getOpcode() == Opcodes.INVOKESPECIAL && ((MethodInsnNode) it).owner.equals(NPE),
                    it -> it.getOpcode() == Opcodes.ATHROW,
                    it -> it instanceof LabelNode,
                    it -> it.getOpcode() == Opcodes.SWAP,
                    it -> it.getOpcode() == Opcodes.DUP,
                    it -> it.getOpcode() == Opcodes.IFNULL,
                    it -> it.getOpcode() == Opcodes.INVOKEVIRTUAL && ((MethodInsnNode) it).name.equals("getClass"),
                    it -> it.getOpcode() == Opcodes.INVOKEVIRTUAL && ((MethodInsnNode) it).name.equals("isAssignableFrom"),
                    it -> it.getOpcode() == Opcodes.GOTO,
                    it -> it instanceof LabelNode,
                    it -> it.getOpcode() == Opcodes.POP,
                    it -> it.getOpcode() == Opcodes.POP,
                    it -> it.getOpcode() == Opcodes.ICONST_0,
                    it -> it instanceof LabelNode
            );
        }

        if (InjectorUtils.isDupedFactoryRedirect(node)) wrap:{
            AbstractInsnNode ldc = InjectorUtils.findFactoryRedirectThrowString(target, currentTarget);
            if (ldc == null) break wrap;
            // We need to encompass the null check added by RedirectInjector.
            checkAndMoveNodes(
                    target.insns,
                    insns,
                    currentTarget,
                    it -> it.getOpcode() == Opcodes.DUP,
                    it -> it.getOpcode() == Opcodes.IFNONNULL,
                    it -> it.getOpcode() == Opcodes.NEW && ((TypeInsnNode) it).desc.equals(NPE),
                    it -> it.getOpcode() == Opcodes.DUP,
                    it -> it == ldc,
                    it -> it.getOpcode() == Opcodes.INVOKESPECIAL && ((MethodInsnNode) it).name.equals("<init>"),
                    it -> it.getOpcode() == Opcodes.ATHROW,
                    it -> it instanceof LabelNode
            );
        }

        return insns;
    }

    @SafeVarargs
    private final void checkAndMoveNodes(InsnList from, InsnList to, AbstractInsnNode node, Predicate<AbstractInsnNode>... predicates) {
        AbstractInsnNode current = node.getNext();
        for (Predicate<AbstractInsnNode> predicate : predicates) {
            if (!predicate.test(current)) {
                throw new AssertionError("Failed assertion when wrapping instructions. Please inform LlamaLad7!");
            }
            AbstractInsnNode old = current;
            do {
                current = current.getNext();
            } while (current instanceof FrameNode);
            from.remove(old);
            to.add(old);
        }
    }

    private Type getReturnType(InjectionNode node) {
        AbstractInsnNode originalTarget = node.getOriginalTarget();
        AbstractInsnNode currentTarget = node.getCurrentTarget();

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
        return getEffectiveArgTypes(node.getOriginalTarget());
    }

    private Type[] getCurrentArgTypes(InjectionNode node) {
        return getEffectiveArgTypes(node.getCurrentTarget());
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
            return new Type[]{Type.getType(Object.class)};
        }

        throw new UnsupportedOperationException();
    }

    private String getName(AbstractInsnNode node) {
        if (node instanceof MethodInsnNode) {
            MethodInsnNode methodInsnNode = (MethodInsnNode) node;
            if (methodInsnNode.name.equals("<init>")) {
                String desc = methodInsnNode.owner;
                return "new" + desc.substring(desc.lastIndexOf('/') + 1);
            }
            return ((MethodInsnNode) node).name;
        }
        if (node instanceof FieldInsnNode) {
            return ((FieldInsnNode) node).name;
        }
        if (node.getOpcode() == Opcodes.INSTANCEOF) {
            String desc = ((TypeInsnNode) node).desc;
            return "instanceof" + desc.substring(desc.lastIndexOf('/') + 1);
        }

        throw new UnsupportedOperationException();
    }
}
