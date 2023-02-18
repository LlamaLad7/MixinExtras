package com.llamalad7.mixinextras.sugar.passback.impl;

import org.apache.commons.lang3.ArrayUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes.InjectionNode;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.util.Bytecode;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public class PassBackInfo {
    private final boolean isVirtual;
    private int nextValueIndex;
    private String owner;
    private final List<Type> values = new ArrayList<>();
    private final List<Consumer<InsnList>> loadRoutines = new ArrayList<>();
    private final List<BiConsumer<InsnList, IntConsumer>> passBackRoutines = new ArrayList<>();
    private final Map<Integer, Integer> valueIndexMap = new HashMap<>();

    public PassBackInfo(boolean isVirtual) {
        this.isVirtual = isVirtual;
    }

    public boolean isVirtual() {
        return isVirtual;
    }

    public void addValue(int paramLvtIndex, Type type, Consumer<InsnList> loadRoutine) {
        values.add(type);
        loadRoutines.add(loadRoutine);
        valueIndexMap.put(paramLvtIndex, nextValueIndex++);
    }

    public void addPassBackStage(BiConsumer<InsnList, IntConsumer> passBackRoutine) {
        passBackRoutines.add(passBackRoutine);
    }

    public String getDescriptor() {
        return Bytecode.getDescriptor(values.toArray(new Type[0]));
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getOwner() {
        return owner;
    }

    public void cleanup() {
        // Only loadRoutines need to be kept, as they're used when transforming overrides of the handler.
        passBackRoutines.clear();
    }

    public void applyToHandler(MethodNode handler, int passBackIndex) {
        for (ListIterator<AbstractInsnNode> it = handler.instructions.iterator(); it.hasNext(); ) {
            AbstractInsnNode insn = it.next();
            if (insn.getOpcode() >= Opcodes.IRETURN && insn.getOpcode() <= Opcodes.RETURN) {
                it.remove();
                it.add(new VarInsnNode(Opcodes.ALOAD, passBackIndex));
                InsnList insns = new InsnList();
                for (Consumer<InsnList> loadRoutine : loadRoutines) {
                    loadRoutine.accept(insns);
                }
                addAll(it, insns);
                it.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        owner,
                        "write",
                        Type.getMethodDescriptor(Type.VOID_TYPE, ArrayUtils.add(values.toArray(new Type[0]), 0, Type.getObjectType(owner))),
                        false
                ));
                it.add(insn);
            }
        }
    }

    public void applyToTarget(Target target, InjectionNode node) {
        int passBackIndex = target.allocateLocal();
        target.addLocalVariable(passBackIndex, "passBack" + passBackIndex, 'L' + owner + ';');
        target.insertBefore(node, new InsnList() {{
            add(new TypeInsnNode(Opcodes.NEW, owner));
            add(new InsnNode(Opcodes.DUP));
            add(new InsnNode(Opcodes.DUP));
            add(new MethodInsnNode(Opcodes.INVOKESPECIAL, owner, "<init>", "()V", false));
            add(new VarInsnNode(Opcodes.ASTORE, passBackIndex));
        }});
        AbstractInsnNode passBackEnd = node.hasDecoration("mixinExtras_passBackEnd")
                ? node.getDecoration("mixinExtras_passBackEnd")
                : node.getCurrentTarget();
        target.insns.insert(passBackEnd, new InsnList() {{
            IntConsumer getValue = lvtIndex -> {
                int valueIndex = valueIndexMap.get(lvtIndex);
                add(new VarInsnNode(Opcodes.ALOAD, passBackIndex));
                add(new FieldInsnNode(Opcodes.GETFIELD, owner, "value" + valueIndex, values.get(valueIndex).getDescriptor()));
            };
            LabelNode end = new LabelNode();
            add(new VarInsnNode(Opcodes.ALOAD, passBackIndex));
            add(new FieldInsnNode(Opcodes.GETFIELD, owner, "isValid", "Z"));
            add(new JumpInsnNode(Opcodes.IFEQ, end));
            for (BiConsumer<InsnList, IntConsumer> passBackRoutine : passBackRoutines) {
                passBackRoutine.accept(this, getValue);
            }
            add(end);
            node.decorate("mixinExtras_passBackEnd", end);
        }});
    }

    private void addAll(ListIterator<AbstractInsnNode> it, InsnList insns) {
        for (ListIterator<AbstractInsnNode> iter = insns.iterator(); iter.hasNext(); ) {
            AbstractInsnNode newInsn = iter.next();
            it.add(newInsn);
        }
    }
}
