package com.llamalad7.mixinextras.expression.impl.flow.expansion;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.flow.postprocessing.FlowPostProcessor;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;
import com.llamalad7.mixinextras.expression.impl.utils.ExpressionDecorations;
import com.llamalad7.mixinextras.utils.CompatibilityHelper;
import com.llamalad7.mixinextras.utils.InjectorUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes.InjectionNode;
import org.spongepowered.asm.mixin.injection.struct.Target;

import java.util.*;
import java.util.function.Consumer;

public abstract class InsnExpander implements FlowPostProcessor {
    private static final String INSN_COMPONENT = "expandedInsnComponent";
    private static final String COMPOUND_INSN = "compoundInsn";
    private static final String INSN_EXPANDER = "insnExpander";

    @Override
    public abstract void process(FlowValue node, FlowPostProcessor.OutputSink sink);

    public abstract void expand(Target target, InjectionNode node, Expansion expansion);

    protected final void registerComponent(FlowValue node, InsnComponent component, AbstractInsnNode compound) {
        node.decorate(INSN_COMPONENT, component);
        node.decorate(COMPOUND_INSN, compound);
        node.decorate(INSN_EXPANDER, this);
    }

    protected final void expandInsn(Target target, InjectionNode node, AbstractInsnNode... insns) {
        InsnList insnList = new InsnList();
        for (AbstractInsnNode insn : insns) {
            insnList.add(insn);
        }
        AbstractInsnNode insn = node.getCurrentTarget();
        target.insns.insert(insn, insnList);
        target.replaceNode(insn, new InsnNode(Opcodes.NOP));
    }

    public static Expansion prepareExpansion(FlowValue node, Target target, InjectionInfo info, ExpressionContext ctx) {
        InsnExpander expander = node.getDecoration(INSN_EXPANDER);
        if (expander == null) {
            return null;
        }
        checkSupportsExpansion(info, ctx.type);
        AbstractInsnNode compoundInsn = node.getDecoration(COMPOUND_INSN);
        InjectionNode compoundNode = target.addInjectionNode(compoundInsn);
        Expansion expansion = compoundNode.getDecoration(ExpressionDecorations.EXPANSION_INFO);
        if (expansion == null) {
            expansion = expander.new Expansion(compoundInsn);
            compoundNode.decorate(ExpressionDecorations.EXPANSION_INFO, expansion);
        }
        expansion.registerInterest(info, node.getDecoration(INSN_COMPONENT));
        return expansion;
    }

    public static InjectionNode doExpansion(InjectionNode node, Target target, InjectionInfo info) {
        Expansion expansion = node.getDecoration(ExpressionDecorations.EXPANSION_INFO);
        if (expansion == null) {
            return node;
        }
        expansion.doExpansion(target, node);
        return target.addInjectionNode(expansion.getTargetInsn(info));
    }

    private static void checkSupportsExpansion(InjectionInfo info, ExpressionContext.Type type) {
        switch (type) {
            case SLICE:
            case INJECT:
            case MODIFY_VARIABLE:
                // Tolerate, they don't care about their target instruction.
                return;
            case MODIFY_EXPRESSION_VALUE:
            case WRAP_OPERATION:
                // Supported.
                return;
        }
        throw CompatibilityHelper.makeInvalidInjectionException(
                info,
                String.format(
                        "Expression context type %s does not support compound instructions!",
                        type
                )
        );
    }

    public static AbstractInsnNode getRepresentative(FlowValue expanded) {
        return expanded.getDecoration(COMPOUND_INSN);
    }

    public class Expansion {
        private final Map<InjectionInfo, InsnComponent> interests = new IdentityHashMap<>();
        private final Map<InsnComponent, List<Consumer<InjectionNode>>> expansionSteps = new HashMap<>();
        private final Map<InsnComponent, AbstractInsnNode> expandedInsns = new HashMap<>();
        private boolean expanded = false;
        public final AbstractInsnNode compound;

        public Expansion(AbstractInsnNode compound) {
            this.compound = compound;
        }

        public void registerInterest(InjectionInfo info, InsnComponent component) {
            if (interests.put(info, component) != null) {
                throw new UnsupportedOperationException("The same injector should not target multiple parts of a compound instruction!");
            }
        }

        public void decorate(InjectionInfo info, String key, Object value) {
            addExpansionStep(interests.get(info), node -> node.decorate(key, value));
        }

        public void decorateInjectorSpecific(InjectionInfo info, String key, Object value) {
            addExpansionStep(
                    interests.get(info),
                    node -> InjectorUtils.decorateInjectorSpecific(node, info, key, value)
            );
        }

        public Set<InsnComponent> registeredInterests() {
            return new HashSet<>(interests.values());
        }

        private void addExpansionStep(InsnComponent component, Consumer<InjectionNode> step) {
            expansionSteps.computeIfAbsent(component, k -> new ArrayList<>()).add(step);
        }

        void doExpansion(Target target, InjectionNode node) {
            if (expanded) {
                return;
            }
            expanded = true;
            InsnExpander.this.expand(target, node, this);
            for (Map.Entry<InsnComponent, List<Consumer<InjectionNode>>> steps : expansionSteps.entrySet()) {
                InjectionNode newNode = target.addInjectionNode(expandedInsns.get(steps.getKey()));
                for (Consumer<InjectionNode> step : steps.getValue()) {
                    step.accept(newNode);
                }
            }
            expansionSteps.clear();
        }

        AbstractInsnNode getTargetInsn(InjectionInfo info) {
            return expandedInsns.get(interests.get(info));
        }

        protected AbstractInsnNode registerInsn(InsnComponent component, AbstractInsnNode insn) {
            expandedInsns.put(component, insn);
            return insn;
        }
    }

    public interface InsnComponent {
    }
}
