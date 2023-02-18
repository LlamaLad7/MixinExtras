package com.llamalad7.mixinextras.sugar.impl;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.SugarBridge;
import com.llamalad7.mixinextras.sugar.passback.impl.PassBackClassGenerator;
import com.llamalad7.mixinextras.sugar.passback.impl.PassBackInfo;
import com.llamalad7.mixinextras.sugar.passback.impl.PassBackVisitor;
import com.llamalad7.mixinextras.utils.ASMUtils;
import com.llamalad7.mixinextras.utils.MixinInternals;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes.InjectionNode;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.transformer.ClassInfo;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.asm.ASM;

import java.util.*;
import java.util.stream.Collectors;

class SugarInjector {
    private static final String SUGAR_PACKAGE = Type.getDescriptor(Local.class).substring(0, Type.getDescriptor(Local.class).lastIndexOf('/') + 1);
    private static final Map<String, SugarInfo> PREPARED_MIXINS = new HashMap<>();

    private final InjectionInfo injectionInfo;
    private final IMixinInfo mixin;
    private final MethodNode handler;
    private final PassBackInfo passBackInfo;
    private Map<Target, List<InjectionNode>> targets;
    private final List<SugarParameter> strippedSugars = new ArrayList<>();
    private final List<SugarApplicator> applicators = new ArrayList<>();
    private final List<SugarApplicationException> exceptions = new ArrayList<>();

    SugarInjector(InjectionInfo injectionInfo, IMixinInfo mixin, MethodNode handler) {
        this.injectionInfo = injectionInfo;
        this.mixin = mixin;
        this.handler = handler;
        this.passBackInfo = PREPARED_MIXINS.get(mixin.getClassName()).getPassBack(handler);
    }

    void setTargets(Map<Target, List<InjectionNode>> targets) {
        this.targets = targets;
    }

    static void prepareMixin(IMixinInfo mixinInfo, ClassNode mixinNode) {
        if (PREPARED_MIXINS.containsKey(mixinInfo.getClassName())) {
            // Don't scan the whole class again.
            return;
        }
        applySuperMixinSugar(mixinInfo, mixinNode);

        SugarInfo sugarInfo = null;
        for (MethodNode method : mixinNode.methods) {
            if (hasSugar(method)) {
                if (sugarInfo == null) {
                    sugarInfo = new SugarInfo();
                }
                wrapInjectorAnnotation(mixinInfo, method);
                preparePassBack(sugarInfo, method, mixinInfo);
            }
        }
        PREPARED_MIXINS.put(mixinInfo.getClassName(), sugarInfo);
    }

    private static void applySuperMixinSugar(IMixinInfo mixinInfo, ClassNode mixinNode) {
        List<Pair<IMixinInfo, ClassNode>> superMixins = MixinInternals.getSuperMixins(mixinInfo);
        if (superMixins.isEmpty()) {
            return;
        }
        Pair<IMixinInfo, ClassNode> parent = superMixins.get(0);
        prepareMixin(parent.getLeft(), parent.getRight());
        for (Pair<IMixinInfo, ClassNode> superMixin : superMixins) {
            SugarInfo sugarInfo = PREPARED_MIXINS.get(superMixin.getLeft().getClassName());
            if (sugarInfo == null) {
                continue;
            }
            for (MethodNode method : mixinNode.methods) {
                PassBackInfo passBackInfo = sugarInfo.getPassBack(method);
                if (passBackInfo != null && passBackInfo.isVirtual()) {
                    applyPassBack(passBackInfo, mixinNode, method, false);
                }
            }
        }
    }

    private static boolean hasSugar(MethodNode method) {
        List<AnnotationNode>[] annotations = method.invisibleParameterAnnotations;
        if (annotations == null) {
            return false;
        }
        for (List<AnnotationNode> paramAnnotations : annotations) {
            if (isSugar(paramAnnotations)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSugar(List<AnnotationNode> paramAnnotations) {
        if (paramAnnotations == null) {
            return false;
        }
        for (AnnotationNode annotation : paramAnnotations) {
            if (annotation.desc.startsWith(SUGAR_PACKAGE)) {
                return true;
            }
        }
        return false;
    }

    private static void wrapInjectorAnnotation(IMixinInfo mixin, MethodNode method) {
        AnnotationNode injectorAnnotation = InjectionInfo.getInjectorAnnotation(mixin, method);
        if (injectorAnnotation == null) {
            return;
        }
        AnnotationNode wrapped = new AnnotationNode(Type.getDescriptor(SugarWrapper.class));
        wrapped.visit("original", injectorAnnotation);
        method.visibleAnnotations.remove(injectorAnnotation);
        method.visibleAnnotations.add(wrapped);
    }

    private static void preparePassBack(SugarInfo sugarInfo, MethodNode handler, IMixinInfo mixin) {
        List<PassBackVisitor> visitors = new ArrayList<>();
        for (SugarParameter sugar : findSugars(handler)) {
            PassBackVisitor visitor = PassBackVisitor.create(sugar);
            if (visitor.isRequired()) {
                visitors.add(visitor);
            }
        }
        if (visitors.isEmpty()) {
            return;
        }
        PassBackInfo passBackInfo = new PassBackInfo(Bytecode.isVirtual(handler));
        for (PassBackVisitor visitor : visitors) {
            visitor.visit(passBackInfo);
        }
        passBackInfo.setOwner(PassBackClassGenerator.getInstance().getForDesc(mixin, passBackInfo.getDescriptor()));
        sugarInfo.addPassBack(handler, passBackInfo);
        // This is needed in the preparation phase so Mixin can mangle overridden injectors properly.
        ClassInfo.forName(mixin.getClassName()).findMethod(handler).remapTo(getDescWithPassBack(passBackInfo, handler));
    }

    void stripSugar() {
        strippedSugars.addAll(findSugars(handler));
        List<Type> params = new ArrayList<>();
        boolean foundSugar = false;
        int i = 0;
        for (Type type : Type.getArgumentTypes(handler.desc)) {
            List<AnnotationNode> annotations = getParamAnnotations(handler, i);
            if (!isSugar(annotations)) {
                if (foundSugar) {
                    throw new IllegalStateException(String.format("Found non-trailing sugared parameters on %s", handler.name + handler.desc));
                }
                params.add(type);
            } else {
                foundSugar = true;
            }
            i++;
        }
        handler.desc = Type.getMethodDescriptor(Type.getReturnType(handler.desc), params.toArray(new Type[0]));
    }

    void prepareSugar() {
        makeApplicators();
        validateApplicators();
        prepareApplicators();
    }

    private void makeApplicators() {
        for (SugarParameter sugar : strippedSugars) {
            SugarApplicator applicator = SugarApplicator.create(injectionInfo, sugar);
            applicators.add(applicator);
        }
    }

    private void validateApplicators() {
        for (SugarApplicator applicator : applicators) {
            for (Map.Entry<Target, List<InjectionNode>> entry : targets.entrySet()) {
                Target target = entry.getKey();
                for (ListIterator<InjectionNode> it = entry.getValue().listIterator(); it.hasNext(); ) {
                    InjectionNode node = it.next();
                    try {
                        applicator.validate(target, node);
                    } catch (SugarApplicationException e) {
                        exceptions.add(
                                new SugarApplicationException(
                                        String.format(
                                                "Failed to validate sugar %s %s on method %s from mixin %s in target method %s at instruction %s",
                                                ASMUtils.annotationToString(applicator.sugar),
                                                ASMUtils.typeToString(applicator.paramType),
                                                handler.name + handler.desc, mixin, target, node
                                        ),
                                        e
                                )
                        );
                        it.remove();
                    }
                }
            }
        }
    }

    private void prepareApplicators() {
        for (Map.Entry<Target, List<InjectionNode>> entry : targets.entrySet()) {
            Target target = entry.getKey();
            for (InjectionNode node : entry.getValue()) {
                try {
                    for (SugarApplicator applicator : applicators) {
                        applicator.prepare(target, node);
                    }
                } catch (Exception e) {
                    throw new SugarApplicationException(
                            String.format(
                                    "Failed to prepare sugar for method %s from mixin %s in target method %s at instruction %s",
                                    handler.name + handler.desc, mixin, target, node
                            ),
                            e
                    );
                }
            }
        }
    }

    List<SugarApplicationException> getExceptions() {
        return exceptions;
    }

    void reSugarHandler() {
        List<Type> paramTypes = new ArrayList<>(Arrays.asList(Type.getArgumentTypes(handler.desc)));
        for (SugarParameter parameter : strippedSugars) {
            paramTypes.add(parameter.type);
        }
        handler.desc = Type.getMethodDescriptor(Type.getReturnType(handler.desc), paramTypes.toArray(new Type[0]));
        if (passBackInfo != null) {
            applyPassBack(passBackInfo, injectionInfo.getClassNode(), handler, true);
        }
    }

    void transformHandlerCalls(Map<Target, List<Pair<InjectionNode, MethodInsnNode>>> calls) {
        for (Map.Entry<Target, List<Pair<InjectionNode, MethodInsnNode>>> entry : calls.entrySet()) {
            Target target = entry.getKey();
            for (Pair<InjectionNode, MethodInsnNode> pair : entry.getValue()) {
                InjectionNode sourceNode = pair.getLeft();
                MethodInsnNode handlerCall = pair.getRight();

                InjectionNode node = target.addInjectionNode(handlerCall);
                Map<String, Object> decorations = MixinInternals.getDecorations(sourceNode);
                if (decorations != null) {
                    for (Map.Entry<String, Object> decoration : decorations.entrySet()) {
                        node.decorate(decoration.getKey(), decoration.getValue());
                    }
                }
                try {
                    for (SugarApplicator applicator : applicators) {
                        applicator.inject(target, node, passBackInfo);
                    }
                    if (passBackInfo != null) {
                        passBackInfo.applyToTarget(target, node);
                    }
                } catch (Exception e) {
                    throw new SugarApplicationException(
                            String.format(
                                    "Failed to apply sugar to method %s from mixin %s in target method %s at instruction %s",
                                    handler.name + handler.desc, mixin, target, node
                            ),
                            e
                    );
                }
                handlerCall.desc = handler.desc;
            }
        }
        if (passBackInfo != null) {
            passBackInfo.cleanup();
        }
    }

    private static List<SugarParameter> findSugars(MethodNode handler) {
        if (handler.invisibleParameterAnnotations == null) {
            return Collections.emptyList();
        }
        List<SugarParameter> result = new ArrayList<>();
        Type[] paramTypes = Type.getArgumentTypes(handler.desc);
        int i = 0;
        int index = Bytecode.isStatic(handler) ? 0 : 1;
        for (Type paramType : paramTypes) {
            List<AnnotationNode> annotationNodes = getParamAnnotations(handler, i);
            AnnotationNode sugar = findSugar(annotationNodes);
            if (sugar != null) {
                result.add(new SugarParameter(sugar, paramType, index));
            }
            i++;
            index += paramType.getSize();
        }
        return result;
    }

    private static AnnotationNode findSugar(List<AnnotationNode> annotations) {
        if (annotations == null) {
            return null;
        }
        AnnotationNode result = null;
        for (AnnotationNode annotation : annotations) {
            if (annotation.desc.startsWith(SUGAR_PACKAGE)) {
                if (result != null) {
                    throw new IllegalStateException(
                            "Found multiple sugars on the same parameter! Got "
                                    + annotations.stream().map(ASMUtils::annotationToString).collect(Collectors.joining(" "))
                    );
                }
                result = annotation;
            }
        }
        return result;
    }

    private static void applyPassBack(PassBackInfo passBackInfo, ClassNode owner, MethodNode handler, boolean generateBridge) {
        int passBackIndex = shiftLocalsInHandler(handler);
        passBackInfo.applyToHandler(handler, passBackIndex);
        String originalDesc = handler.desc;
        String newDesc = getDescWithPassBack(passBackInfo, handler);
        ClassInfo.forName(owner.name).findMethod(handler).remapTo(newDesc);
        handler.desc = newDesc;
        if (generateBridge) {
            owner.methods.add(makePassBackBridge(owner, handler, originalDesc));
        }
    }

    private static String getDescWithPassBack(PassBackInfo passBackInfo, MethodNode handler) {
        Type returnType = Type.getReturnType(handler.desc);
        Type[] arguments = ArrayUtils.add(Type.getArgumentTypes(handler.desc), Type.getObjectType(passBackInfo.getOwner()));
        return Bytecode.getDescriptor(returnType, arguments);
    }

    private static int shiftLocalsInHandler(MethodNode handler) {
        handler.maxLocals++;
        int firstLocal = Bytecode.getFirstNonArgLocalIndex(handler);
        for (ListIterator<AbstractInsnNode> it = handler.instructions.iterator(); it.hasNext(); ) {
            AbstractInsnNode insn = it.next();
            if (insn instanceof VarInsnNode) {
                VarInsnNode varInsn = (VarInsnNode) insn;
                if (varInsn.var >= firstLocal) {
                    varInsn.var++;
                }
            } else if (insn instanceof IincInsnNode) {
                IincInsnNode iincInsn = (IincInsnNode) insn;
                if (iincInsn.var >= firstLocal) {
                    iincInsn.var++;
                }
            }
        }
        if (handler.localVariables != null) {
            for (LocalVariableNode local : handler.localVariables) {
                if (local != null && local.index >= firstLocal) {
                    local.index++;
                }
            }
        }
        return firstLocal;
    }

    private static MethodNode makePassBackBridge(ClassNode owner, MethodNode handler, String originalDesc) {
        MethodNode bridge = new MethodNode(
                ASM.API_VERSION,
                handler.access & ~Opcodes.ACC_ABSTRACT,
                handler.name,
                originalDesc,
                handler.signature,
                handler.exceptions.toArray(new String[0])
        );
        bridge.visitAnnotation(Type.getDescriptor(SugarBridge.class), false);
        bridge.instructions = new InsnList() {{
            boolean isStatic = Bytecode.isStatic(handler);
            if (!isStatic) {
                add(new VarInsnNode(Opcodes.ALOAD, 0));
            }
            int index = isStatic ? 0 : 1;
            for (Type type : Type.getArgumentTypes(originalDesc)) {
                add(new VarInsnNode(type.getOpcode(Opcodes.ILOAD), index));
                index += type.getSize();
            }
            add(new InsnNode(Opcodes.ACONST_NULL));
            add(new MethodInsnNode(
                    isStatic ? Opcodes.INVOKESTATIC : Bytecode.isVirtual(handler) ? Opcodes.INVOKEVIRTUAL : Opcodes.INVOKESPECIAL,
                    owner.name,
                    handler.name,
                    handler.desc,
                    (owner.access & Opcodes.ACC_INTERFACE) != 0
            ));
            add(new InsnNode(Type.getReturnType(originalDesc).getOpcode(Opcodes.IRETURN)));

            bridge.maxLocals = Bytecode.getArgsSize(Type.getArgumentTypes(originalDesc)) + (isStatic ? 0 : 1);
            bridge.maxStack = index + 1;
        }};
        return bridge;
    }

    private static List<AnnotationNode> getParamAnnotations(MethodNode handler, int paramIndex) {
        List<AnnotationNode>[] invisible = handler.invisibleParameterAnnotations;
        if (invisible != null && invisible.length >= paramIndex) {
            return invisible[paramIndex];
        }
        return null;
    }
}
