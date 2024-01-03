package com.llamalad7.mixinextras.expression.impl.point;

import com.llamalad7.mixinextras.expression.Pool;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.Expression;
import com.llamalad7.mixinextras.expression.impl.flow.FlowInterpreter;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool;
import com.llamalad7.mixinextras.utils.CompatibilityHelper;
import com.llamalad7.mixinextras.utils.TargetDecorations;
import com.llamalad7.mixinextras.utils.info.ExtraMixinInfo;
import com.llamalad7.mixinextras.utils.info.ExtraMixinInfoManager;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
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

    public ExpressionInjectionPoint(InjectionPointData data) {
        super(data);
        this.ordinal = data.getOrdinal();
    }

    @Override
    public boolean find(String desc, InsnList insns, Collection<AbstractInsnNode> nodes) {
        if (insns.size() == 0) {
            return false;
        }
        long startTime = System.currentTimeMillis();
        Target target = getTarget(insns);
        Map<AbstractInsnNode, FlowValue> flows = TargetDecorations.getOrPut(target, "ValueFlow", () -> {
            FlowInterpreter interpreter = new FlowInterpreter(CURRENT_INFO.getClassNode(), target.method);
            try {
                new Analyzer<>(interpreter).analyze(CURRENT_INFO.getClassNode().name, target.method);
            } catch (AnalyzerException e) {
                throw new RuntimeException("Failed to analyze value flow: ", e);
            }
            return interpreter.finish();
        });
        AnnotationNode poolAnnotation = Annotations.getInvisible(CURRENT_INFO.getMethod(), Pool.class);
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
                                if (!injectionNode.hasDecoration(decoration.getKey())) {
                                    injectionNode.decorate(decoration.getKey(), new HashMap<>());
                                }
                                Map<InjectionInfo, Object> inner = injectionNode.getDecoration(decoration.getKey());
                                inner.put(CURRENT_INFO, decoration.getValue());
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
                if (expr.matches(flow, pool, sink)) {
                    result.addAll(captured);
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
            }
            i++;
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
        ExtraMixinInfo info = ExtraMixinInfoManager.getInfo(CompatibilityHelper.getMixin(CURRENT_INFO).getMixin());
        AnnotationNode ann = Annotations.getInvisible(CURRENT_INFO.getMethod(), com.llamalad7.mixinextras.expression.Expression.class);
        if (ann == null) {
            throw new IllegalStateException(CURRENT_INFO + " is missing @Expression annotation!");
        }
        List<String> strings = Annotations.getValue(ann, "value", Collections.emptyList());
        return strings.stream().map(info::getExpression).collect(Collectors.toList());
    }
}
