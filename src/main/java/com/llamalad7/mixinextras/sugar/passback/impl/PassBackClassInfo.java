package com.llamalad7.mixinextras.sugar.passback.impl;

import org.apache.commons.lang3.ArrayUtils;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.transformer.SyntheticClassInfo;
import org.spongepowered.asm.util.Bytecode;

public class PassBackClassInfo extends SyntheticClassInfo {
    private boolean isLoaded;
    private final Type[] valueTypes;

    PassBackClassInfo(IMixinInfo mixin, String name, Type[] valueTypes) {
        super(mixin, name);
        this.valueTypes = valueTypes;
    }

    void markAsLoaded() {
        isLoaded = true;
    }

    @Override
    public boolean isLoaded() {
        return isLoaded;
    }

    public String getDesc() {
        return Bytecode.getDescriptor(valueTypes);
    }

    void generateFields(ClassVisitor visitor) {
        visitor.visitField(Opcodes.ACC_PUBLIC, "isValid", "Z", null, null);
        for (int i = 0; i < valueTypes.length; i++) {
            visitor.visitField(Opcodes.ACC_PUBLIC, "value" + i, valueTypes[i].getDescriptor(), null, null);
        }
    }

    void generateWriteMethod(ClassVisitor visitor) {
        MethodNode method = new MethodNode(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "write",
                Bytecode.generateDescriptor(void.class, (Object[]) ArrayUtils.add(valueTypes, 0, Type.getObjectType(name))),
                null,
                null);
        method.instructions = new InsnList() {{
            LabelNode end = new LabelNode();
            add(new VarInsnNode(Opcodes.ALOAD, 0));
            add(new JumpInsnNode(Opcodes.IFNULL, end));
            add(new VarInsnNode(Opcodes.ALOAD, 0));
            add(new InsnNode(Opcodes.ICONST_1));
            add(new FieldInsnNode(Opcodes.PUTFIELD, name, "isValid", "Z"));
            int index = 1;
            for (int i = 0; i < valueTypes.length; i++) {
                Type type = valueTypes[i];
                add(new VarInsnNode(Opcodes.ALOAD, 0));
                add(new VarInsnNode(type.getOpcode(Opcodes.ILOAD), index));
                add(new FieldInsnNode(Opcodes.PUTFIELD, name, "value" + i, type.getDescriptor()));
                index += type.getSize();
            }
            add(end);
            add(new InsnNode(Opcodes.RETURN));
        }};
        method.maxStack = 3;
        method.maxLocals = Bytecode.getArgsSize(valueTypes);
        method.accept(visitor);
    }
}
