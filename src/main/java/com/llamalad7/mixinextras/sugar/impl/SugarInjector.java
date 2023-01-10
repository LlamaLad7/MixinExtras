package com.llamalad7.mixinextras.sugar.impl;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.utils.ASMUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes.InjectionNode;
import org.spongepowered.asm.mixin.injection.struct.Target;

import java.util.*;
import java.util.stream.Collectors;

class SugarInjector {
    private static final String SUGAR_PACKAGE = Type.getDescriptor(Local.class).substring(0, Type.getDescriptor(Local.class).lastIndexOf('/') + 1);
    private static final Set<String> PREPARED_MIXINS = new HashSet<>();

    private final InjectionInfo injectionInfo;
    private final IMixinInfo mixin;
    private Map<Target, List<InjectionNode>> targets;
    private String desugaredHandlerDesc;
    private final MethodNode handler;
    private final List<Type> sugarParams = new ArrayList<>();
    private final List<List<AnnotationNode>> sugarParamAnnotations = new ArrayList<>();
    private final List<SugarApplicator> applicators = new ArrayList<>();
    private final List<SugarApplicationException> exceptions = new ArrayList<>();

    SugarInjector(InjectionInfo injectionInfo, IMixinInfo mixin, MethodNode handler) {
        this.injectionInfo = injectionInfo;
        this.mixin = mixin;
        this.handler = handler;
    }

    void setTargets(Map<Target, List<InjectionNode>> targets) {
        this.targets = targets;
    }

    static void prepareMixin(IMixinInfo mixinInfo, ClassNode mixinNode) {
        if (PREPARED_MIXINS.contains(mixinInfo.getClassName())) {
            // Don't scan the whole class again.
            return;
        }
        for (MethodNode method : mixinNode.methods) {
            if (hasSugar(method)) {
                wrapInjectorAnnotation(mixinInfo, method);
            }
        }
        PREPARED_MIXINS.add(mixinInfo.getClassName());
    }

    private static boolean hasSugar(MethodNode method) {
        List<AnnotationNode>[] annotations = method.invisibleParameterAnnotations;
        if (annotations == null) {
            return false;
        }
        for (List<AnnotationNode> paramAnnotations : annotations) {
            if (paramAnnotations == null) {
                continue;
            }
            for (AnnotationNode annotation : paramAnnotations) {
                if (annotation.desc.startsWith(SUGAR_PACKAGE)) {
                    return true;
                }
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

    @SuppressWarnings("unchecked")
    void stripSugar() {
        List<Type> params = new ArrayList<>();
        List<List<AnnotationNode>> invisibleAnnotations = new ArrayList<>();
        boolean foundSugar = false;
        int i = 0;
        for (Type type : Type.getArgumentTypes(handler.desc)) {
            List<AnnotationNode> annotations = handler.invisibleParameterAnnotations[i];
            if (annotations == null || annotations.stream().noneMatch(it -> it != null && it.desc.startsWith(SUGAR_PACKAGE))) {
                if (foundSugar) {
                    throw new IllegalStateException(String.format("Found non-trailing sugared parameters on %s", handler.name + handler.desc));
                }
                params.add(type);
                invisibleAnnotations.add(annotations);
            } else {
                foundSugar = true;
                sugarParams.add(type);
                sugarParamAnnotations.add(annotations);
            }
            i++;
        }
        handler.invisibleParameterAnnotations = invisibleAnnotations.toArray(new List[0]);
        handler.desc = Type.getMethodDescriptor(Type.getReturnType(handler.desc), params.toArray(new Type[0]));
        desugaredHandlerDesc = handler.desc;
    }

    void prepareSugar() {
        for (Pair<Type, AnnotationNode> sugar : findSugars()) {
            applicators.add(SugarApplicator.create(injectionInfo, sugar.getLeft(), sugar.getRight()));
        }
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
                                            "Failed to validate sugar %s on method %s from mixin %s in target method %s at instruction %s",
                                            ASMUtils.annotationToString(applicator.sugar), handler.name + handler.desc, mixin, target, node
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

    void applySugar() {
        reSugar();
        for (Target target : targets.keySet()) {
            for (MethodInsnNode targetInsn : findHandlerCalls(target)) {
                InjectionNode node = target.addInjectionNode(targetInsn);
                try {
                    for (SugarApplicator applicator : applicators) {
                        applicator.preInject(target, node);
                    }
                    for (SugarApplicator applicator : applicators) {
                        applicator.inject(target, node);
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
                targetInsn.desc = handler.desc;
            }
        }
    }

    List<SugarApplicationException> getExceptions() {
        return exceptions;
    }

    @SuppressWarnings("unchecked")
    private void reSugar() {
        List<Type> paramTypes = new ArrayList<>(Arrays.asList(Type.getArgumentTypes(handler.desc)));
        List<List<AnnotationNode>> paramAnnotations = (List<List<AnnotationNode>>) (Object) new ArrayList<>(
                Arrays.asList(
                        handler.invisibleParameterAnnotations == null ? new List[paramTypes.size()] : handler.invisibleParameterAnnotations
                )
        );
        paramTypes.addAll(sugarParams);
        paramAnnotations.addAll(sugarParamAnnotations);
        handler.desc = Type.getMethodDescriptor(Type.getReturnType(handler.desc), paramTypes.toArray(new Type[0]));
        handler.invisibleParameterAnnotations = paramAnnotations.toArray(new List[0]);
    }

    private List<MethodInsnNode> findHandlerCalls(Target target) {
        List<MethodInsnNode> result = new ArrayList<>();
        for (AbstractInsnNode insn : target) {
            if (insn instanceof MethodInsnNode){
                MethodInsnNode call = (MethodInsnNode) insn;
                if (call.owner.equals(target.classNode.name) && call.name.equals(handler.name) && call.desc.equals(desugaredHandlerDesc)) {
                    result.add(call);
                }
            }
        }
        return result;
    }

    private List<Pair<Type, AnnotationNode>> findSugars() {
        if (handler.invisibleParameterAnnotations == null) {
            return Collections.emptyList();
        }
        List<Pair<Type, AnnotationNode>> result = new ArrayList<>();
        int i = 0;
        for (List<AnnotationNode> annotationNodes : sugarParamAnnotations) {
            AnnotationNode sugar = findSugar(annotationNodes);
            if (sugar != null) {
                result.add(Pair.of(sugarParams.get(i), sugar));
            }
            i++;
        }
        return result;
    }

    private AnnotationNode findSugar(List<AnnotationNode> annotations) {
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
}
