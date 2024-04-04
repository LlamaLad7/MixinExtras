package com.llamalad7.mixinextras.expression.impl.pool;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.util.Annotations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IdentifierPool {
    private final Map<String, List<MemberDefinition>> members = new HashMap<>();
    private final Map<String, List<TypeDefinition>> types = new HashMap<>();

    public IdentifierPool(Target target, InjectionInfo info, AnnotationNode poolAnnotation) {
        this();
        for (AnnotationNode entry : Annotations.<AnnotationNode>getValue(poolAnnotation, "value", true)) {
            parseEntry(entry, target, info);
        }
    }

    public IdentifierPool() {
        addType("byte", new ExactTypeDef(Type.BYTE_TYPE));
        addType("char", new ExactTypeDef(Type.CHAR_TYPE));
        addType("double", new ExactTypeDef(Type.DOUBLE_TYPE));
        addType("float", new ExactTypeDef(Type.FLOAT_TYPE));
        addType("int", new ExactTypeDef(Type.INT_TYPE));
        addType("long", new ExactTypeDef(Type.LONG_TYPE));
        addType("short", new ExactTypeDef(Type.SHORT_TYPE));
        addMember("length", new ArrayLengthDef());
    }

    public boolean matchesMember(String id, AbstractInsnNode insn) {
        List<MemberDefinition> matching = members.get(id);
        if (matching == null) {
            throw new IllegalStateException("Use of undeclared identifier '" + id + '\'');
        }
        return matching.stream().anyMatch(it -> it.matches(insn));
    }

    public boolean matchesMember(String id, Handle handle) {
        List<MemberDefinition> matching = members.get(id);
        if (matching == null) {
            throw new IllegalStateException("Use of undeclared identifier '" + id + '\'');
        }
        return matching.stream().anyMatch(it -> it.matches(handle));
    }

    public boolean matchesType(String id, Type type) {
        List<TypeDefinition> matching = types.get(id);
        if (matching == null) {
            throw new IllegalStateException("Use of undeclared identifier '" + id + '\'');
        }
        return matching.stream().anyMatch(it -> it.matches(type));
    }

    private void parseEntry(AnnotationNode entry, Target target, InjectionInfo info) {
        String id = Annotations.getValue(entry, "id");
        for (String method : Annotations.<String>getValue(entry, "method", true)) {
            addMember(id, new MethodDef(method, info));
        }
        for (String method : Annotations.<String>getValue(entry, "field", true)) {
            addMember(id, new FieldDef(method, info));
        }
        for (Type type : Annotations.<Type>getValue(entry, "type", true)) {
            addType(id, new ExactTypeDef(type));
        }
        for (AnnotationNode local : Annotations.<AnnotationNode>getValue(entry, "local", true)) {
            addMember(id, new LocalDef(local, info, target));
        }
    }

    public void addMember(String id, MemberDefinition entry) {
        members.computeIfAbsent(id, k -> new ArrayList<>()).add(entry);
    }

    public void addType(String id, TypeDefinition entry) {
        types.computeIfAbsent(id, k -> new ArrayList<>()).add(entry);
    }

    public boolean memberExists(String id) {
        return members.containsKey(id);
    }

    public boolean typeExists(String id) {
        return types.containsKey(id);
    }
}
