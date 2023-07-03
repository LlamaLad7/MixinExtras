package com.llamalad7.mixinextras.sugar.impl.ref;

import com.llamalad7.mixinextras.sugar.impl.ref.generated.GeneratedImplDummy;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.llamalad7.mixinextras.utils.ClassGenUtils;
import com.llamalad7.mixinextras.utils.PackageUtils;
import org.apache.commons.lang3.StringUtils;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import java.util.HashMap;
import java.util.Map;

/**
 * We must generate implementations of {@link LocalRef} and co. at runtime so they implement all the required interfaces.
 * These objects will be shared between handlers that possibly use different relocations of MixinExtras.
 */
public class LocalRefClassGenerator {
    private static final String IMPL_PACKAGE = StringUtils.substringBeforeLast(LocalRefClassGenerator.class.getName(), ".").replace('.', '/') + "/generated";
    private static final Map<Class<?>, String> interfaceToImpl = new HashMap<>();

    public static String getForType(Type type) {
        Class<?> refInterface = LocalRefUtils.getInterfaceFor(type);
        String owner = interfaceToImpl.get(refInterface);
        if (owner != null) {
            return owner;
        }
        owner = IMPL_PACKAGE + '/' + StringUtils.substringAfterLast(refInterface.getName(), ".") + "Impl";
        String desc = type.getDescriptor();
        String innerDesc = desc.length() == 1 ? desc : Type.getDescriptor(Object.class);
        interfaceToImpl.put(refInterface, owner);
        ClassNode node = new ClassNode();
        node.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, owner, null, Type.getInternalName(Object.class), null);
        generateClass(node, owner, innerDesc, refInterface.getName());
        ClassGenUtils.defineClass(node, GeneratedImplDummy.getLookup());
        return owner;
    }

    private static void generateClass(ClassNode node, String owner, String innerDesc, String interfaceName) {
        for (String name : PackageUtils.getAllClassNames(interfaceName)) {
            node.interfaces.add(name.replace('.', '/'));
        }
        node.visitField(Opcodes.ACC_PRIVATE, "value", innerDesc, null, null);

        MethodVisitor ctor = node.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "(" + innerDesc + ")V", null, null);
        ctor.visitCode();
        ctor.visitVarInsn(Opcodes.ALOAD, 0);
        ctor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(Object.class), "<init>", "()V", false);
        ctor.visitVarInsn(Opcodes.ALOAD, 0);
        ctor.visitVarInsn(Type.getType(innerDesc).getOpcode(Opcodes.ILOAD), 1);
        ctor.visitFieldInsn(Opcodes.PUTFIELD, owner, "value", innerDesc);
        ctor.visitInsn(Opcodes.RETURN);
        ctor.visitMaxs(3, 3);
        ctor.visitEnd();

        MethodVisitor getter = node.visitMethod(Opcodes.ACC_PUBLIC, "get", "()" + innerDesc, null, null);
        getter.visitCode();
        getter.visitVarInsn(Opcodes.ALOAD, 0);
        getter.visitFieldInsn(Opcodes.GETFIELD, owner, "value", innerDesc);
        getter.visitInsn(Type.getType(innerDesc).getOpcode(Opcodes.IRETURN));
        getter.visitMaxs(2, 1);
        getter.visitEnd();

        MethodVisitor setter = node.visitMethod(Opcodes.ACC_PUBLIC, "set", "(" + innerDesc + ")V", null, null);
        setter.visitCode();
        setter.visitVarInsn(Opcodes.ALOAD, 0);
        setter.visitVarInsn(Type.getType(innerDesc).getOpcode(Opcodes.ILOAD), 1);
        setter.visitFieldInsn(Opcodes.PUTFIELD, owner, "value", innerDesc);
        setter.visitInsn(Opcodes.RETURN);
        setter.visitMaxs(3, 3);
        setter.visitEnd();
    }
}
