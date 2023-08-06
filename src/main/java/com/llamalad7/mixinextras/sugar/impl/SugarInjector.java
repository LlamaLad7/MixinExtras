package com.llamalad7.mixinextras.sugar.impl;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.impl.handlers.HandlerInfo;
import com.llamalad7.mixinextras.sugar.impl.handlers.HandlerTransformer;
import com.llamalad7.mixinextras.utils.ASMUtils;
import com.llamalad7.mixinextras.utils.Decorations;
import com.llamalad7.mixinextras.utils.MixinInternals;
import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes.InjectionNode;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.util.Bytecode;

import java.util.*;
import java.util.stream.Collectors;

class SugarInjector {
//    private static final String SUGAR_PACKAGE = Type.getDescriptor(Local.class).substring(0, Type.getDescriptor(Local.class).lastIndexOf('/') + 1);
    private static final Set<ClassNode> PREPARED_MIXINS = Collections.newSetFromMap(new WeakHashMap<>());

    private final InjectionInfo injectionInfo;
    private final IMixinInfo mixin;
    private final MethodNode handler;
    private final List<Type> parameterGenerics;
    private Map<Target, List<InjectionNode>> targets;
    private final List<SugarParameter> strippedSugars = new ArrayList<>();
    private final List<SugarApplicator> applicators = new ArrayList<>();
    private final List<SugarApplicationException> exceptions = new ArrayList<>();

    SugarInjector(InjectionInfo injectionInfo, IMixinInfo mixin, MethodNode handler, List<Type> parameterGenerics) {
        this.injectionInfo = injectionInfo;
        this.mixin = mixin;
        this.handler = handler;
        this.parameterGenerics = parameterGenerics;
    }

    void setTargets(Map<Target, List<InjectionNode>> targets) {
        this.targets = targets;
    }

    static void prepareMixin(IMixinInfo mixinInfo, ClassNode mixinNode) {
        if (PREPARED_MIXINS.contains(mixinNode)) {
            // Don't scan the whole class again.
            return;
        }

        for (MethodNode method : mixinNode.methods) {
            if (hasSugar(method)) {
                wrapInjectorAnnotation(mixinInfo, method);
            }
        }
        PREPARED_MIXINS.add(mixinNode);
    }

    static HandlerInfo getHandlerInfo(IMixinInfo mixin, MethodNode handler, List<Type> generics) {
        List<HandlerTransformer> transformers = new ArrayList<>();
        for (SugarParameter sugar : findSugars(handler, generics)) {
            HandlerTransformer transformer = HandlerTransformer.create(mixin, sugar);
            if (transformer != null && transformer.isRequired(handler)) {
                transformers.add(transformer);
            }
        }
        if (transformers.isEmpty()) {
            return null;
        }

        HandlerInfo handlerInfo = new HandlerInfo();
        for (HandlerTransformer transformer : transformers) {
            transformer.transform(handlerInfo);
        }
        return handlerInfo;
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
            if (SugarApplicator.isSugar(annotation.desc)) {
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
        wrapped.visit("signature", method.signature == null ? "" : method.signature);
        method.visibleAnnotations.remove(injectorAnnotation);
        method.visibleAnnotations.add(wrapped);
    }

    void stripSugar() {
        strippedSugars.addAll(findSugars(handler, parameterGenerics));
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
                                                handler, mixin, target, node
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
                                    handler, mixin, target, node
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
    }

    void transformHandlerCalls(Map<Target, List<Pair<InjectionNode, MethodInsnNode>>> calls) {
        for (Map.Entry<Target, List<Pair<InjectionNode, MethodInsnNode>>> entry : calls.entrySet()) {
            Target target = entry.getKey();
            for (Pair<InjectionNode, MethodInsnNode> pair : entry.getValue()) {
                InjectionNode sourceNode = pair.getLeft();
                MethodInsnNode handlerCall = pair.getRight();

                InjectionNode node = target.addInjectionNode(handlerCall);
                Map<String, Object> decorations = MixinInternals.getDecorations(sourceNode);
                for (Map.Entry<String, Object> decoration : decorations.entrySet()) {
                    if (decoration.getKey().startsWith(Decorations.PERSISTENT)) {
                        node.decorate(decoration.getKey(), decoration.getValue());
                    }
                }
                try {
                    for (SugarApplicator applicator : applicators) {
                        applicator.inject(target, node);
                    }
                } catch (Exception e) {
                    throw new SugarApplicationException(
                            String.format(
                                    "Failed to apply sugar to method %s from mixin %s in target method %s at instruction %s",
                                    handler, mixin, target, node
                            ),
                            e
                    );
                }
                handlerCall.desc = handler.desc;
            }
        }
    }

    private static List<SugarParameter> findSugars(MethodNode handler, List<Type> generics) {
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
                result.add(new SugarParameter(sugar, paramType, generics.get(i), index, i));
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
            if (SugarApplicator.isSugar(annotation.desc)) {
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

    private static List<AnnotationNode> getParamAnnotations(MethodNode handler, int paramIndex) {
        List<AnnotationNode>[] invisible = handler.invisibleParameterAnnotations;
        if (invisible != null && invisible.length >= paramIndex) {
            return invisible[paramIndex];
        }
        return null;
    }
}
