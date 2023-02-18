package com.llamalad7.mixinextras.sugar.passback.impl;

import com.llamalad7.mixinextras.sugar.passback.synthetic.PassBackMarker;
import com.llamalad7.mixinextras.utils.MixinInternals;
import org.objectweb.asm.ClassVisitor;
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

public class PassBackClassGenerator implements IClassGenerator {
    private static final String CLASS_NAME_BASE = PassBackMarker.class.getName().replace("Marker", "_");
    private static PassBackClassGenerator INSTANCE;

    private final IConsumer<ISyntheticClassInfo> registry;
    private int nextIndex = 1;
    private final Map<String, PassBackClassInfo> descToInfo = new HashMap<>();
    private final Map<String, PassBackClassInfo> nameToInfo = new HashMap<>();

    public PassBackClassGenerator(IConsumer<ISyntheticClassInfo> registry) {
        this.registry = registry;
        INSTANCE = this;
    }

    @Override
    public String getName() {
        return "MixinExtras PassBack";
    }

    @Override
    public boolean generate(String name, ClassNode classNode) {
        PassBackClassInfo info = nameToInfo.get(name);
        if (info == null) {
            return false;
        }
        classNode.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, info.getName(), null,
                Type.getInternalName(Object.class), null);
        this.generateCtor(classNode);
        info.generateFields(classNode);
        info.generateWriteMethod(classNode);
        classNode.visitEnd();
        info.markAsLoaded();
        return true;
    }

    public String getForDesc(IMixinInfo mixin, String desc) {
        PassBackClassInfo info = descToInfo.get(desc);
        if (info != null) {
            return info.getName();
        }
        info = new PassBackClassInfo(mixin, CLASS_NAME_BASE + nextIndex++, Type.getArgumentTypes(desc));
        descToInfo.put(desc, info);
        nameToInfo.put(info.getClassName(), info);
        registry.accept(info);
        ClassNode classNode = new ClassNode();
        generate(info.getClassName(), classNode);
        // Mixin chokes if it sees a reference to a PassBack when transforming the mixin.
        // This isn't needed for its own synthetics because they're only added by injectors.
        MixinInternals.registerClassInfo(classNode);
        return info.getName();
    }

    private void generateCtor(ClassVisitor visitor) {
        MethodVisitor ctor = visitor.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        ctor.visitCode();
        ctor.visitVarInsn(Opcodes.ALOAD, 0);
        ctor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(Object.class), "<init>", "()V", false);
        ctor.visitInsn(Opcodes.RETURN);
        ctor.visitMaxs(1, 1);
        ctor.visitEnd();
    }

    public static PassBackClassGenerator getInstance() {
        return INSTANCE;
    }
}
