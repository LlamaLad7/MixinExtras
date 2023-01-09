package com.llamalad7.mixinextras.sugar.impl;

import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.utils.ASMUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes.InjectionNode;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.util.Annotations;

import java.util.*;
import java.util.stream.Collectors;

public class SugarInjector {
    private static final String SUGAR_PACKAGE = Type.getDescriptor(Local.class).substring(0, Type.getDescriptor(Local.class).lastIndexOf('/') + 1);
    private static final Set<String> PREPARED_MIXINS = new HashSet<>();

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
    static void stripSugar(MethodNode method) {
        List<Type> params = new ArrayList<>();
        List<Type> sugarParams = new ArrayList<>();
        List<List<AnnotationNode>> invisibleAnnotations = new ArrayList<>();
        List<List<AnnotationNode>> sugarParamAnnotations = new ArrayList<>();
        boolean foundSugar = false;
        int i = 0;
        for (Type type : Type.getArgumentTypes(method.desc)) {
            List<AnnotationNode> annotations = method.invisibleParameterAnnotations[i];
            if (annotations == null || annotations.stream().noneMatch(it -> it != null && it.desc.startsWith(SUGAR_PACKAGE))) {
                if (foundSugar) {
                    throw new IllegalStateException(String.format("Found non-trailing sugared parameters on %s", method.name + method.desc));
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
        if (method.invisibleAnnotations == null) {
            method.invisibleAnnotations = new ArrayList<>();
        }
        method.invisibleAnnotations.add(buildMarkerAnnotation(method, sugarParams, sugarParamAnnotations));
        method.invisibleParameterAnnotations = invisibleAnnotations.toArray(new List[0]);
        method.desc = Type.getMethodDescriptor(Type.getReturnType(method.desc), params.toArray(new Type[0]));
    }

    private static AnnotationNode buildMarkerAnnotation(MethodNode method, List<Type> sugarParams, List<List<AnnotationNode>> sugarParamAnnotations) {
        AnnotationNode infoHolder = new AnnotationNode(Type.getDescriptor(SugarInfoHolder.class));
        List<AnnotationNode> params = new ArrayList<>();
        int i = 0;
        for (Type paramType : sugarParams) {
            AnnotationNode info = new AnnotationNode(Type.getDescriptor(SugarInfoHolder.Info.class));
            info.visit("type", paramType);
            info.visit("annotations", sugarParamAnnotations.get(i));
            params.add(info);
            i++;
        }
        infoHolder.visit("value", params);
        boolean isWrapOperation = Annotations.getVisible(method, WrapOperation.class) != null;
        if (isWrapOperation) {
            infoHolder.visit("applyLate", true);
        }
        return infoHolder;
    }

    static void applySugar(InjectionInfo injectionInfo, IMixinInfo mixin, Collection<Target> targets, MethodNode handler) {
        String oldDesc = handler.desc;
        reSugarMethod(handler);
        List<Pair<Type, AnnotationNode>> sugars = findSugars(handler);
        for (Target target : targets) {
            for (MethodInsnNode targetInsn : findCalls(target, handler, oldDesc)) {
                InjectionNode node = target.addInjectionNode(targetInsn);
                try {
                    SugarApplicator.preApply(injectionInfo, sugars, target, node);
                    for (Pair<Type, AnnotationNode> sugarInfo : sugars) {
                        SugarApplicator.apply(injectionInfo, sugarInfo.getLeft(), sugarInfo.getRight(), target, node);
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

    @SuppressWarnings("unchecked")
    private static void reSugarMethod(MethodNode handler) {
        AnnotationNode infoHolder = getAndRemoveSugarInfo(handler);
        if (infoHolder == null) {
            return;
        }
        List<Type> paramTypes = new ArrayList<>(Arrays.asList(Type.getArgumentTypes(handler.desc)));
        List<List<AnnotationNode>> paramAnnotations = (List<List<AnnotationNode>>) (Object) new ArrayList<>(
                Arrays.asList(
                        handler.invisibleParameterAnnotations == null ? new List[paramTypes.size()] : handler.invisibleParameterAnnotations
                )
        );
        for (AnnotationNode info : Annotations.<List<AnnotationNode>>getValue(infoHolder)) {
            paramTypes.add(Annotations.getValue(info, "type"));
            paramAnnotations.add(Annotations.getValue(info, "annotations"));
        }
        handler.desc = Type.getMethodDescriptor(Type.getReturnType(handler.desc), paramTypes.toArray(new Type[0]));
        handler.invisibleParameterAnnotations = paramAnnotations.toArray(new List[0]);
    }

    private static AnnotationNode getAndRemoveSugarInfo(MethodNode method) {
        if (method.invisibleAnnotations == null) {
            return null;
        }
        String desc = Type.getDescriptor(SugarInfoHolder.class);
        for (ListIterator<AnnotationNode> it = method.invisibleAnnotations.listIterator(); it.hasNext(); ) {
            AnnotationNode next = it.next();
            if (next.desc.equals(desc)) {
                it.remove();
                return next;
            }
        }
        return null;
    }

    private static List<MethodInsnNode> findCalls(Target target, MethodNode method, String desc) {
        List<MethodInsnNode> result = new ArrayList<>();
        for (AbstractInsnNode insn : target) {
            if (insn instanceof MethodInsnNode){
                MethodInsnNode call = (MethodInsnNode) insn;
                if (call.owner.equals(target.classNode.name) && call.name.equals(method.name) && call.desc.equals(desc)) {
                    result.add(call);
                }
            }
        }
        return result;
    }

    private static List<Pair<Type, AnnotationNode>> findSugars(MethodNode method) {
        if (method.invisibleParameterAnnotations == null) {
            return Collections.emptyList();
        }
        List<Pair<Type, AnnotationNode>> result = new ArrayList<>();
        Type[] paramTypes = Type.getArgumentTypes(method.desc);
        int i = 0;
        for (List<AnnotationNode> annotationNodes : method.invisibleParameterAnnotations) {
            AnnotationNode sugar = findSugar(annotationNodes);
            if (sugar != null) {
                result.add(Pair.of(paramTypes[i], sugar));
            }
            i++;
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
}
