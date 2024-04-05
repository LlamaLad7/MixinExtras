package com.llamalad7.mixinextras.expression.impl.flow.expansion;

import com.llamalad7.mixinextras.expression.impl.flow.FlowPostProcessor;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes;
import org.spongepowered.asm.mixin.injection.struct.Target;

class IincExpander extends InsnExpander {
    @Override
    public void expand(FlowValue node, FlowPostProcessor.OutputSink sink) {
        if (node.getInsn().getOpcode() != Opcodes.IINC) {
            return;
        }
        IincInsnNode iinc = (IincInsnNode) node.getInsn();
        FlowValue load = new FlowValue(Type.INT_TYPE, new VarInsnNode(Opcodes.ILOAD, iinc.var));
        registerComponent(load, Component.LOAD, iinc);
        FlowValue cst = new FlowValue(Type.INT_TYPE, new IntInsnNode(Opcodes.BIPUSH, iinc.incr));
        registerComponent(cst, Component.CST, iinc);
        FlowValue add = new FlowValue(Type.INT_TYPE, new InsnNode(Opcodes.IADD), load, cst);
        registerComponent(add, Component.ADD, iinc);

        node.setInsn(new VarInsnNode(Opcodes.ISTORE, iinc.var));
        node.setParents(add);
        registerComponent(node, Component.STORE, iinc);

        sink.registerFlow(load, cst, add);
    }

    @Override
    public void expand(Target target, InjectionNodes.InjectionNode node, Expansion expansion) {
        IincInsnNode iinc = (IincInsnNode) node.getCurrentTarget();
        expandInsn(
                target, node,
                expansion.registerInsn(Component.LOAD, new VarInsnNode(Opcodes.ILOAD, iinc.var)),
                expansion.registerInsn(Component.CST, new IntInsnNode(Opcodes.BIPUSH, iinc.incr)),
                expansion.registerInsn(Component.ADD, new InsnNode(Opcodes.IADD)),
                expansion.registerInsn(Component.STORE, new VarInsnNode(Opcodes.ISTORE, iinc.var))
        );
    }

    private enum Component implements InsnComponent {
        LOAD, CST, ADD, STORE
    }
}
