package com.llamalad7.mixinextras.sugar.impl;

import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.utils.MixinInternals;
import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes.InjectionNode;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.transformer.ClassInfo;
import org.spongepowered.asm.mixin.transformer.meta.MixinMerged;
import org.spongepowered.asm.util.Annotations;

import java.util.*;

public class SugarInjector {
    private static final String SUGAR_PACKAGE = Type.getDescriptor(Local.class).substring(0, Type.getDescriptor(Local.class).lastIndexOf('/') + 1);
    private static final Set<String> TARGET_CLASSES_WITH_SUGAR = new HashSet<>();
    private static final Map<String, IMixinInfo> TRANSFORMED_MIXINS = new HashMap<>();
    private static final Set<String> SUGAR_FREE_MIXINS = new HashSet<>();

    static void preApply(String targetClassName, ClassNode mixinNode, ClassInfo mixinClassInfo, IMixinInfo mixinInfo) {
        if (SUGAR_FREE_MIXINS.contains(mixinInfo.getClassName())) {
            // No sugar here, don't scan the whole class again.
            return;
        }
        if (TRANSFORMED_MIXINS.containsKey(mixinInfo.getClassName())) {
            // This mixin contains sugar, and we've already transformed it.
            TARGET_CLASSES_WITH_SUGAR.add(targetClassName);
            return;
        }
        boolean hasAnySugar = false;
        for (MethodNode method : mixinNode.methods) {
            if (hasSugar(method)) {
                hasAnySugar = true;
                stripSugar(method, mixinClassInfo);
            }
        }
        if (hasAnySugar) {
            TARGET_CLASSES_WITH_SUGAR.add(targetClassName);
            TRANSFORMED_MIXINS.put(mixinInfo.getClassName(), mixinInfo);
        } else {
            SUGAR_FREE_MIXINS.add(mixinInfo.getClassName());
        }
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

    private static void stripSugar(MethodNode method, ClassInfo mixinInfo) {
        String newDesc = stripSugar(method);
        MixinInternals.setCurrentDesc(mixinInfo.findMethod(method), newDesc);
        method.desc = newDesc;
    }

    @SuppressWarnings("unchecked")
    private static String stripSugar(MethodNode method) {
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
                    throw new IllegalStateException(String.format("Found non-trailing sugared parameters on %s", method.name));
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
        return Type.getMethodDescriptor(Type.getReturnType(method.desc), params.toArray(new Type[0]));
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

    static void postApply(ClassNode targetClass) {
        if (!TARGET_CLASSES_WITH_SUGAR.remove(targetClass.name)) {
            return;
        }
        List<MethodNode> methods = discoverSugaredMethods(targetClass);
        Map<String, MethodNode> sugaredMethods = reSugarMethods(targetClass, methods);
        for (MethodNode method : targetClass.methods) {
            applySugarToMethod(method, sugaredMethods, targetClass);
        }
    }

    public static void applyFromInjector(ClassNode targetClass, MethodNode handler, MethodNode targetMethod) {
        if (Annotations.getInvisible(handler, SugarInfoHolder.class) == null) {
            return;
        }
        Map<String, MethodNode> sugaredMethod = reSugarMethods(targetClass, Collections.singletonList(handler));
        applySugarToMethod(targetMethod, sugaredMethod, targetClass);
    }

    private static List<MethodNode> discoverSugaredMethods(ClassNode targetClass) {
        List<MethodNode> result = new ArrayList<>();
        for (MethodNode method : targetClass.methods) {
            AnnotationNode infoHolder = Annotations.getInvisible(method, SugarInfoHolder.class);
            if (infoHolder != null && !Annotations.getValue(infoHolder, "applyLate", (Boolean) false)) {
                result.add(method);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, MethodNode> reSugarMethods(ClassNode targetClass, List<MethodNode> methods) {
        Map<String, MethodNode> result = new HashMap<>();
        for (MethodNode method : methods) {
            AnnotationNode infoHolder = getAndRemoveSugarInfo(method);
            if (infoHolder == null) {
                continue;
            }
            result.put(method.name + method.desc, method);
            List<Type> paramTypes = new ArrayList<>(Arrays.asList(Type.getArgumentTypes(method.desc)));
            List<List<AnnotationNode>> paramAnnotations = (List<List<AnnotationNode>>) (Object) new ArrayList<>(
                    Arrays.asList(
                            method.invisibleParameterAnnotations == null ? new List[paramTypes.size()] : method.invisibleParameterAnnotations
                    )
            );
            for (AnnotationNode info : Annotations.<List<AnnotationNode>>getValue(infoHolder)) {
                paramTypes.add(Annotations.getValue(info, "type"));
                paramAnnotations.add(Annotations.getValue(info, "annotations"));
            }
            String newDesc = Type.getMethodDescriptor(Type.getReturnType(method.desc), paramTypes.toArray(new Type[0]));
            MixinInternals.setCurrentDesc(ClassInfo.forName(targetClass.name).findMethod(method), newDesc);
            method.desc = newDesc;
            method.invisibleParameterAnnotations = paramAnnotations.toArray(new List[0]);
        }
        return result;
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

    private static void applySugarToMethod(MethodNode method, Map<String, MethodNode> sugaredMethods, ClassNode targetClass) {
        List<Pair<MethodInsnNode, MethodNode>> targets = new ArrayList<>();
        for (ListIterator<AbstractInsnNode> it = method.instructions.iterator(); it.hasNext(); ) {
            AbstractInsnNode insn = it.next();
            if (insn instanceof MethodInsnNode) {
                MethodInsnNode methodInsn = (MethodInsnNode) insn;
                MethodNode targetMethod = sugaredMethods.get(methodInsn.name + methodInsn.desc);
                if (targetMethod != null && methodInsn.owner.equals(targetClass.name)) {
                    targets.add(Pair.of(methodInsn, targetMethod));
                }
            }
        }
        if (targets.isEmpty()) {
            return;
        }
        Target target = new Target(targetClass, method);
        for (Pair<MethodInsnNode, MethodNode> targetPair : targets) {
            MethodInsnNode targetInsn = targetPair.getLeft();
            MethodNode targetMethod = targetPair.getRight();
            AnnotationNode mixinMerged = Annotations.getVisible(targetMethod, MixinMerged.class);
            IMixinInfo mixin = TRANSFORMED_MIXINS.get(Annotations.<String>getValue(mixinMerged, "mixin"));
            InjectionNode node = target.addInjectionNode(targetInsn);
            List<Pair<Type, AnnotationNode>> sugars = findSugars(targetMethod);
            SugarApplicator.preApply(mixin, sugars, target, node);
            for (Pair<Type, AnnotationNode> sugarInfo : sugars) {
                SugarApplicator.apply(mixin, sugarInfo.getLeft(), sugarInfo.getRight(), target, node);
            }
            targetInsn.desc = targetMethod.desc;
        }
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
                    throw new IllegalStateException("Found multiple sugars!");
                }
                result = annotation;
            }
        }
        return result;
    }
}
