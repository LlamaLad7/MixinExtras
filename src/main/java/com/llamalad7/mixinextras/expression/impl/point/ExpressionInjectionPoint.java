package com.llamalad7.mixinextras.expression.impl.point;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.impl.ExpressionParserFacade;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.Expression;
import com.llamalad7.mixinextras.expression.impl.flow.ComplexDataException;
import com.llamalad7.mixinextras.expression.impl.flow.FlowInterpreter;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool;
import com.llamalad7.mixinextras.utils.ASMUtils;
import com.llamalad7.mixinextras.utils.CompatibilityHelper;
import com.llamalad7.mixinextras.utils.InjectorUtils;
import com.llamalad7.mixinextras.utils.TargetDecorations;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.InjectionPoint.AtCode;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes.InjectionNode;
import org.spongepowered.asm.mixin.injection.struct.InjectionPointData;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.util.Annotations;

import java.util.*;
import java.util.stream.Collectors;

@AtCode("MIXINEXTRAS:EXPRESSION")
public class ExpressionInjectionPoint extends InjectionPoint {
    private static List<Target> CURRENT_TARGETS;
    private static InjectionInfo CURRENT_INFO;

    public static long TIME_ON_EXPRESSIONS = 0;

    private final int ordinal;
    private final String id;

    public ExpressionInjectionPoint(InjectionPointData data) {
        super(data);
        this.ordinal = data.getOrdinal();
        this.id = data.getId() != null ? data.getId() : "";
    }

    @Override
    public boolean find(String desc, InsnList insns, Collection<AbstractInsnNode> nodes) {
        if (insns.size() == 0) {
            return false;
        }
        long startTime = System.currentTimeMillis();
        Target target = getTarget(insns);
        Map<AbstractInsnNode, FlowValue> flows =
                TargetDecorations.getOrPut(target, "ValueFlow",
                        () -> FlowInterpreter.analyze(CURRENT_INFO.getClassNode(), target.method)
                );
        AnnotationNode poolAnnotation = ASMUtils.getRepeatedMEAnnotation(CURRENT_INFO.getMethod(), Definition.class);
        IdentifierPool pool = new IdentifierPool(target, CURRENT_INFO, poolAnnotation);
        Set<AbstractInsnNode> result = new HashSet<>();
        for (Expression expr : parseExpressions()) {
            for (AbstractInsnNode candidate : target) {
                Map<AbstractInsnNode, Map<String, Object>> genericDecorations = new IdentityHashMap<>();
                Map<AbstractInsnNode, Map<String, Object>> injectorSpecificDecorations = new IdentityHashMap<>();
                List<AbstractInsnNode> captured = new ArrayList<>();

                Expression.OutputSink sink = new Expression.OutputSink() {
                    @Override
                    public void capture(AbstractInsnNode insn) {
                        Map<String, Object> decorations = genericDecorations.get(insn);
                        if (decorations != null) {
                            InjectionNode injectionNode = target.addInjectionNode(insn);
                            for (Map.Entry<String, Object> decoration : decorations.entrySet()) {
                                injectionNode.decorate(decoration.getKey(), decoration.getValue());
                            }
                        }
                        Map<String, Object> injectorSpecific = injectorSpecificDecorations.get(insn);
                        if (injectorSpecific != null) {
                            InjectionNode injectionNode = target.addInjectionNode(insn);
                            for (Map.Entry<String, Object> decoration : injectorSpecific.entrySet()) {
                                InjectorUtils.decorateInjectorSpecific(
                                        injectionNode,
                                        CURRENT_INFO,
                                        decoration.getKey(),
                                        decoration.getValue()
                                );
                            }
                        }
                        captured.add(insn);
                    }

                    @Override
                    public void decorate(AbstractInsnNode insn, String key, Object value) {
                        genericDecorations.computeIfAbsent(insn, k -> new HashMap<>()).put(key, value);
                    }

                    @Override
                    public void decorateInjectorSpecific(AbstractInsnNode insn, String key, Object value) {
                        injectorSpecificDecorations.computeIfAbsent(insn, k -> new HashMap<>()).put(key, value);
                    }
                };

                FlowValue flow = flows.get(candidate);
                if (flow == null) {
                    continue;
                }
                try {
                    if (expr.matches(flow, new ExpressionContext(pool, sink, target))) {
                        result.addAll(captured);
                    }
                } catch (ComplexDataException ignored) {
                }
            }
        }
        int i = 0;
        boolean found = false;
        for (ListIterator<AbstractInsnNode> it = insns.iterator(); it.hasNext(); ) {
            AbstractInsnNode insn = it.next();

            if (result.contains(insn)) {
                if (ordinal < 0 || ordinal == i) {
                    nodes.add(insn);
                    found = true;
                }
                i++;
            }
        }

        TIME_ON_EXPRESSIONS += System.currentTimeMillis() - startTime;

        return found;
    }

    public static void withContext(InjectionInfo info, Runnable runnable) {
        InjectionInfo oldInfo = CURRENT_INFO;
        List<Target> oldTargets = CURRENT_TARGETS;
        try {
            CURRENT_INFO = info;
            CURRENT_TARGETS = CompatibilityHelper.getTargets(info);
            runnable.run();
        } finally {
            CURRENT_INFO = oldInfo;
            CURRENT_TARGETS = oldTargets;
        }
    }

    /**
     * Hacky way to get the {@link Target} even though we're not passed it.
     */
    private Target getTarget(InsnList insns) {
        AbstractInsnNode marker = insns.getFirst();
        Target target = null;
        for (Target candidate : CURRENT_TARGETS) {
            if (candidate.method.instructions.contains(marker)) {
                target = candidate;
                break;
            }
        }
        if (target == null) {
            throw new IllegalStateException("Could not find target for " + insns);
        }
        // This target is the least likely to be used again so push it to the back:
        CURRENT_TARGETS.remove(target);
        CURRENT_TARGETS.add(target);
        return target;
    }

    private List<Expression> parseExpressions() {
        List<String> strings = getMatchingExpressions(CURRENT_INFO.getMethod());
        return strings.stream().map(ExpressionParserFacade::parse).collect(Collectors.toList());
    }

    private List<String> getMatchingExpressions(MethodNode method) {
        List<String> result = new ArrayList<>();
        AnnotationNode expressions = ASMUtils.getRepeatedMEAnnotation(method, com.llamalad7.mixinextras.expression.Expression.class);
        for (AnnotationNode expression : Annotations.<AnnotationNode>getValue(expressions, "value", true)) {
            if (Annotations.getValue(expression, "id", "").equals(this.id)) {
                result.addAll(Annotations.getValue(expression, "value", true));
            }
        }
        if (result.isEmpty()) {
            String idText = id.isEmpty() ? "" : "for id '" + id + "' ";
            throw new IllegalStateException("No expression found " + idText + "on " + CURRENT_INFO);
        }
        return result;
    }
}
