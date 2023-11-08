package com.llamalad7.mixinextras.expression.impl.pool;

import org.objectweb.asm.Opcodes;
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
    private final Map<String, List<PoolEntry>> pool = new HashMap<>();

    public IdentifierPool(Target target, InjectionInfo info, AnnotationNode poolAnnotation) {
        this(target);
        for (AnnotationNode entry : Annotations.<AnnotationNode>getValue(poolAnnotation, "value", true)) {
            parseEntry(entry, target, info);
        }
    }

    IdentifierPool(Target target) {
        addEntry("byte", new PrimitiveCastPoolEntry(Opcodes.I2B));
        addEntry("char", new PrimitiveCastPoolEntry(Opcodes.I2C));
        addEntry("double", new PrimitiveCastPoolEntry(Opcodes.I2D, Opcodes.L2D, Opcodes.F2D));
        addEntry("float", new PrimitiveCastPoolEntry(Opcodes.I2F, Opcodes.L2F, Opcodes.D2F));
        addEntry("int", new PrimitiveCastPoolEntry(Opcodes.L2I, Opcodes.F2I, Opcodes.D2I));
        addEntry("long", new PrimitiveCastPoolEntry(Opcodes.I2L, Opcodes.F2L, Opcodes.D2L));
        addEntry("short", new PrimitiveCastPoolEntry(Opcodes.I2S));
        addEntry("length", new ArrayLengthPoolEntry());
        if (!target.isStatic) {
            addEntry("this", new ThisPoolEntry());
        }
    }

    public boolean matches(String id, AbstractInsnNode insn) {
        List<PoolEntry> matching = pool.get(id);
        if (matching == null) {
            throw new IllegalStateException("Use of undeclared identifier '" + id + '\'');
        }
        return matching.stream().anyMatch(it -> it.matches(insn));
    }

    private void parseEntry(AnnotationNode entry, Target target, InjectionInfo info) {
        String id = Annotations.getValue(entry, "id");
        for (AnnotationNode at : Annotations.<AnnotationNode>getValue(entry, "at", true)) {
            addEntry(id, new AtPoolEntry(at, info, target));
        }
        for (Type type : Annotations.<Type>getValue(entry, "type", true)) {
            addEntry(id, new TypePoolEntry(type));
        }
    }

    private void addEntry(String id, PoolEntry entry) {
        pool.computeIfAbsent(id, k -> new ArrayList<>()).add(entry);
    }
}
