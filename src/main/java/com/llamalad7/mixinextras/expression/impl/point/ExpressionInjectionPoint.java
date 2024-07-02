package com.llamalad7.mixinextras.expression.impl.point;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.impl.ExpressionParserFacade;
import com.llamalad7.mixinextras.expression.impl.ExpressionService;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.Expression;
import com.llamalad7.mixinextras.expression.impl.flow.ComplexDataException;
import com.llamalad7.mixinextras.expression.impl.flow.FlowInterpreter;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.flow.expansion.InsnExpander;
import com.llamalad7.mixinextras.expression.impl.pool.BytecodeIdentifierPool;
import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool;
import com.llamalad7.mixinextras.expression.impl.utils.FlowDecorations;
import com.llamalad7.mixinextras.injector.ModifyExpressionValueInjectionInfo;
import com.llamalad7.mixinextras.injector.ModifyReceiverInjectionInfo;
import com.llamalad7.mixinextras.injector.ModifyReturnValueInjectionInfo;
import com.llamalad7.mixinextras.injector.v2.WrapWithConditionInjectionInfo;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperationInjectionInfo;
import com.llamalad7.mixinextras.service.MixinExtrasVersion;
import com.llamalad7.mixinextras.utils.*;
import com.llamalad7.mixinextras.wrapper.WrapperInjectionInfo;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.InjectionPoint.AtCode;
import org.spongepowered.asm.mixin.injection.struct.*;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes.InjectionNode;
import org.spongepowered.asm.util.Annotations;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

@AtCode("MIXINEXTRAS:EXPRESSION")
public class ExpressionInjectionPoint extends InjectionPoint {
    private static List<Target> CURRENT_TARGETS;
    private static InjectionInfo CURRENT_INFO;

    public static long TIME_ON_EXPRESSIONS = 0;

    private final int ordinal;
    private final String id;
    private final boolean isInSlice;

    private boolean initialized;
    private IdentifierPool pool;
    private List<Expression> expressions;
    private ExpressionContext.Type contextType;

    static {
        ExpressionService.offerInstance(new RuntimeExpressionService());
    }

    public ExpressionInjectionPoint(InjectionPointData data) {
        super(data);
        this.ordinal = data.getOrdinal();
        this.id = data.getId() != null ? data.getId() : "";
        this.isInSlice = data.get(Decorations.IS_IN_SLICE, false);
    }

    @Override
    public boolean find(String desc, InsnList insns, Collection<AbstractInsnNode> nodes) {
        if (insns.size() == 0) {
            return false;
        }
        long startTime = System.currentTimeMillis();
        Target target = getTarget(insns);
        if (!initialized) {
            initialize(target);
        }
        Collection<FlowValue> flows =
                TargetDecorations.getOrPut(target, "ValueFlow",
                        () -> FlowInterpreter.analyze(CURRENT_INFO.getClassNode(), target.method, null)
                );
        Set<AbstractInsnNode> result = new HashSet<>();

        Map<AbstractInsnNode, Map<String, Object>> genericDecorations = new IdentityHashMap<>();
        Map<AbstractInsnNode, Map<String, Object>> injectorSpecificDecorations = new IdentityHashMap<>();
        List<AbstractInsnNode> captured = new ArrayList<>();
        Expression.OutputSink sink = new Expression.OutputSink() {
            @Override
            public void capture(FlowValue node, Expression expr, ExpressionContext ctx) {
                AbstractInsnNode capturedInsn = node.getInsn();
                InsnExpander.Expansion expansion = InsnExpander.prepareExpansion(node, target, CURRENT_INFO, ctx);
                AbstractInsnNode targetInsn;
                BiConsumer<String, Object> decorate;
                BiConsumer<String, Object> decorateInjectorSpecific;
                if (expansion != null) {
                    targetInsn = expansion.compound;
                    decorate = (k, v) -> expansion.decorate(CURRENT_INFO, k, v);
                    decorateInjectorSpecific = (k, v) -> expansion.decorateInjectorSpecific(CURRENT_INFO, k, v);
                } else {
                    targetInsn = node.getInsn();
                    InjectionNode injectionNode = target.addInjectionNode(capturedInsn);
                    decorate = injectionNode::decorate;
                    decorateInjectorSpecific = (k, v) -> InjectorUtils.decorateInjectorSpecific(injectionNode, CURRENT_INFO, k, v);
                }
                Map<String, Object> decorations = genericDecorations.get(capturedInsn);
                if (decorations != null) {
                    for (Map.Entry<String, Object> decoration : decorations.entrySet()) {
                        decorate.accept(decoration.getKey(), decoration.getValue());
                    }
                }
                Map<String, Object> injectorSpecific = injectorSpecificDecorations.get(capturedInsn);
                if (injectorSpecific != null) {
                    for (Map.Entry<String, Object> decoration : injectorSpecific.entrySet()) {
                        decorateInjectorSpecific.accept(decoration.getKey(), decoration.getValue());
                    }
                }
                for (Map.Entry<String, Object> decoration : node.getDecorations().entrySet()) {
                    if (decoration.getKey().startsWith(FlowDecorations.PERSISTENT)) {
                        decorate.accept(decoration.getKey(), decoration.getValue());
                    }
                }
                captured.add(targetInsn);
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
        ExpressionContext ctx = new ExpressionContext(
                pool,
                sink,
                target.classNode,
                target.method,
                contextType,
                false
        );

        for (Expression expr : expressions) {
            for (FlowValue flow : flows) {
                try {
                    if (expr.matches(flow, ctx)) {
                        result.addAll(captured);
                    }
                } catch (ComplexDataException ignored) {
                }
                genericDecorations.clear();
                injectorSpecificDecorations.clear();
                captured.clear();
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

    private void initialize(Target target) {
        checkDeclaredMinVersion();
        initialized = true;
        AnnotationNode poolAnnotation = ASMUtils.getRepeatedMEAnnotation(CURRENT_INFO.getMethod(), Definition.class);
        pool = new BytecodeIdentifierPool(target, CURRENT_INFO, poolAnnotation);
        expressions = parseExpressions();
        contextType = selectContextType();
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

    private void checkDeclaredMinVersion() {
        IMixinConfig config = CompatibilityHelper.getMixin(CURRENT_INFO).getMixin().getConfig();
        MixinExtrasVersion min = MixinConfigUtils.minVersionFor(config);
        if (min.getNumber() < MixinExtrasVersion.V0_5_0_BETA_1.getNumber()) {
            throw new UnsupportedOperationException(
                    String.format(
                            "In order to use Expressions, Mixin Config '%s' needs to declare a \"%s\"" +
                                    " of at least %s! E.g. `\"%2$s\": \"%s\"`",
                            config, MixinConfigUtils.KEY_MIN_VERSION, MixinExtrasVersion.V0_5_0_BETA_1, MixinExtrasVersion.LATEST
                    )
            );
        }
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

    private ExpressionContext.Type selectContextType() {
        if (isInSlice) {
            return ExpressionContext.Type.SLICE;
        }
        InjectionInfo info = CURRENT_INFO;
        while (info instanceof WrapperInjectionInfo) {
            info = ((WrapperInjectionInfo) info).getDelegate();
        }
        if (info instanceof CallbackInjectionInfo) {
            return ExpressionContext.Type.INJECT;
        }
        if (info instanceof ModifyArgInjectionInfo) {
            return ExpressionContext.Type.MODIFY_ARG;
        }
        if (info instanceof ModifyArgsInjectionInfo) {
            return ExpressionContext.Type.MODIFY_ARGS;
        }
        if (info instanceof ModifyConstantInjectionInfo) {
            return ExpressionContext.Type.MODIFY_CONSTANT;
        }
        if (info instanceof ModifyExpressionValueInjectionInfo) {
            return ExpressionContext.Type.MODIFY_EXPRESSION_VALUE;
        }
        if (info instanceof ModifyReceiverInjectionInfo) {
            return ExpressionContext.Type.MODIFY_RECEIVER;
        }
        if (info instanceof ModifyReturnValueInjectionInfo) {
            return ExpressionContext.Type.MODIFY_RETURN_VALUE;
        }
        if (info instanceof ModifyVariableInjectionInfo) {
            return ExpressionContext.Type.MODIFY_VARIABLE;
        }
        if (info instanceof RedirectInjectionInfo) {
            return ExpressionContext.Type.REDIRECT;
        }
        if (info instanceof WrapOperationInjectionInfo) {
            return ExpressionContext.Type.WRAP_OPERATION;
        }
        if (info instanceof WrapWithConditionInjectionInfo) {
            return ExpressionContext.Type.WRAP_WITH_CONDITION;
        }
        return ExpressionContext.Type.CUSTOM;
    }
}
