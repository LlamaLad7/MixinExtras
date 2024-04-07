package com.llamalad7.mixinextras.expression.impl.flow.expansion;

import com.llamalad7.mixinextras.expression.impl.flow.postprocessing.FlowPostProcessor;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.utils.CompatibilityHelper;
import com.llamalad7.mixinextras.utils.Decorations;
import com.llamalad7.mixinextras.utils.InjectorUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.spongepowered.asm.mixin.injection.struct.CallbackInjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes.InjectionNode;
import org.spongepowered.asm.mixin.injection.struct.ModifyVariableInjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.Target;

import java.util.*;
import java.util.function.Consumer;

public abstract class InsnExpander {
    private static final String INSN_COMPONENT = "expandedInsnComponent";
    private static final String COMPOUND_INSN = "compoundInsn";
    private static final String INSN_EXPANDER = "insnExpander";

    public abstract void expand(FlowValue node, FlowPostProcessor.OutputSink sink);

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

    public static Expansion prepareExpansion(FlowValue node, Target target, InjectionInfo info) {
        checkSupportsExpansion(info);
        InsnExpander expander = node.getDecoration(INSN_EXPANDER);
        if (expander == null) {
            return null;
        }
        AbstractInsnNode compoundInsn = node.getDecoration(COMPOUND_INSN);
        InjectionNode compoundNode = target.addInjectionNode(compoundInsn);
        Expansion expansion = compoundNode.getDecoration(Decorations.EXPANSION_INFO);
        if (expansion == null) {
            expansion = expander.new Expansion(compoundInsn);
            compoundNode.decorate(Decorations.EXPANSION_INFO, expansion);
        }
        expansion.registerInterest(info, node.getDecoration(INSN_COMPONENT));
        return expansion;
    }

    public static InjectionNode doExpansion(InjectionNode node, Target target, InjectionInfo info) {
        Expansion expansion = node.getDecoration(Decorations.EXPANSION_INFO);
        if (expansion == null) {
            return node;
        }
        expansion.doExpansion(target, node);
        return target.addInjectionNode(expansion.getTargetInsn(info));
    }

    private static void checkSupportsExpansion(InjectionInfo info) {
        if (info instanceof CallbackInjectionInfo || info instanceof ModifyVariableInjectionInfo) {
            // Tolerate, they don't care about their target instruction and so will just place code before it.
            return;
        }
        if (!(info instanceof SupportsInsnExpansion)) {
            throw CompatibilityHelper.makeInvalidInjectionException(
                    info,
                    String.format(
                            "Injector %s does not support compound instructions!",
                            info
                    )
            );
        }
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
            interests.put(info, component);
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
