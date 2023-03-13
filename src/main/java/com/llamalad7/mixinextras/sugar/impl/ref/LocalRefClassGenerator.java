package com.llamalad7.mixinextras.sugar.impl.ref;

import com.llamalad7.mixinextras.sugar.ref.*;
import com.llamalad7.mixinextras.utils.PackageUtils;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.transformer.ext.IClassGenerator;
import org.spongepowered.asm.service.ISyntheticClassInfo;
import org.spongepowered.asm.util.IConsumer;

import java.util.HashMap;
import java.util.Map;

/**
 * We must generate implementations of {@link LocalRef} and co. at runtime so they implement all the required interfaces.
 * These objects will be shared between handlers that possibly use different relocations of MixinExtras.
 */
public class LocalRefClassGenerator implements IClassGenerator {
    private static LocalRefClassGenerator INSTANCE;

    private final IConsumer<ISyntheticClassInfo> registry;
    private final Map<Class<?>, LocalRefClassInfo> interfaceToInfo = new HashMap<>();
    private final Map<String, LocalRefClassInfo> nameToInfo = new HashMap<>();

    public LocalRefClassGenerator(IConsumer<ISyntheticClassInfo> registry) {
        this.registry = registry;
        INSTANCE = this;
    }

    @Override
    public String getName() {
        return "MixinExtras LocalRefImpl";
    }

    @Override
    public boolean generate(String name, ClassNode classNode) {
        LocalRefClassInfo info = nameToInfo.get(name);
        if (info == null) {
            return false;
        }
        classNode.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, info.getName(), null,
                Type.getInternalName(Object.class), null);
        this.generateClass(classNode, info);
        classNode.visitEnd();
        info.markAsLoaded();
        return true;
    }

    public String getForType(IMixinInfo mixin, Type type) {
        Class<?> refInterface = LocalRefUtils.getInterfaceFor(type);
        LocalRefClassInfo info = interfaceToInfo.get(refInterface);
        if (info != null) {
            return info.getName();
        }
        String desc = type.getDescriptor();
        info = new LocalRefClassInfo(mixin, refInterface, desc.length() == 1 ? desc : Type.getDescriptor(Object.class));
        interfaceToInfo.put(refInterface, info);
        nameToInfo.put(info.getClassName(), info);
        registry.accept(info);
        return info.getName();
    }

    private void generateClass(ClassNode node, LocalRefClassInfo info) {
        for (String name : PackageUtils.getAllClassNames(info.getInterfaceName())) {
            node.interfaces.add(name.replace('.', '/'));
        }
        node.visitField(Opcodes.ACC_PRIVATE, "value", info.getDesc(), null, null);

        MethodVisitor ctor = node.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "(" + info.getDesc() + ")V", null, null);
        ctor.visitCode();
        ctor.visitVarInsn(Opcodes.ALOAD, 0);
        ctor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(Object.class), "<init>", "()V", false);
        ctor.visitVarInsn(Opcodes.ALOAD, 0);
        ctor.visitVarInsn(Type.getType(info.getDesc()).getOpcode(Opcodes.ILOAD), 1);
        ctor.visitFieldInsn(Opcodes.PUTFIELD, info.getName(), "value", info.getDesc());
        ctor.visitInsn(Opcodes.RETURN);
        ctor.visitMaxs(3, 3);
        ctor.visitEnd();

        MethodVisitor getter = node.visitMethod(Opcodes.ACC_PUBLIC, "get", "()" + info.getDesc(), null, null);
        getter.visitCode();
        getter.visitVarInsn(Opcodes.ALOAD, 0);
        getter.visitFieldInsn(Opcodes.GETFIELD, info.getName(), "value", info.getDesc());
        getter.visitInsn(Type.getType(info.getDesc()).getOpcode(Opcodes.IRETURN));
        getter.visitMaxs(2, 1);
        getter.visitEnd();

        MethodVisitor setter = node.visitMethod(Opcodes.ACC_PUBLIC, "set", "(" + info.getDesc() + ")V", null, null);
        setter.visitCode();
        setter.visitVarInsn(Opcodes.ALOAD, 0);
        setter.visitVarInsn(Type.getType(info.getDesc()).getOpcode(Opcodes.ILOAD), 1);
        setter.visitFieldInsn(Opcodes.PUTFIELD, info.getName(), "value", info.getDesc());
        setter.visitInsn(Opcodes.RETURN);
        setter.visitMaxs(3, 3);
        setter.visitEnd();
    }

    public static LocalRefClassGenerator getInstance() {
        return INSTANCE;
    }
}
