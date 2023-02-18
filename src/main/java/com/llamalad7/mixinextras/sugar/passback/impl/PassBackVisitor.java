package com.llamalad7.mixinextras.sugar.passback.impl;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.impl.SugarParameter;
import org.objectweb.asm.Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public abstract class PassBackVisitor {
    private static final Map<String, Class<? extends PassBackVisitor>> MAP = new HashMap<>();

    static {
        MAP.put(Type.getDescriptor(Local.class), LocalPassBackVisitor.class);
    }

    protected final SugarParameter parameter;

    PassBackVisitor(SugarParameter parameter) {
        this.parameter = parameter;
    }

    public abstract boolean isRequired();

    public abstract void visit(PassBackInfo info);

    public static PassBackVisitor create(SugarParameter parameter) {
        try {
            Class<? extends PassBackVisitor> clazz = MAP.get(parameter.sugar.desc);
            Constructor<? extends PassBackVisitor> ctor = clazz.getDeclaredConstructor(SugarParameter.class);
            return ctor.newInstance(parameter);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
