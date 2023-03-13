package com.llamalad7.mixinextras.sugar.impl.handlers;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.impl.SugarParameter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * Handler transformers belong to an individual sugar parameter and can transform {@link HandlerInfo}s.
 */
public abstract class HandlerTransformer {
    private static final Map<String, Class<? extends HandlerTransformer>> MAP = new HashMap<>();

    static {
        MAP.put(Type.getDescriptor(Local.class), LocalHandlerTransformer.class);
    }

    protected final IMixinInfo mixin;
    protected final SugarParameter parameter;

    HandlerTransformer(IMixinInfo mixin, SugarParameter parameter) {
        this.mixin = mixin;
        this.parameter = parameter;
    }

    /**
     * Determines whether this transformer needs to make any changes to the given handler method.
     * If none of the transformers are required, the handler transformation process can be skipped entirely.
     * @param handler the handler method
     * @return whether this transformer needs to make changes to the handler
     */
    public abstract boolean isRequired(MethodNode handler);

    /**
     * Describes the required changes to the handler method.
     * This is only called if the transformer returned true in {@link HandlerTransformer#isRequired}.
     * @param info the handler to be transformed
     */
    public abstract void transform(HandlerInfo info);

    public static HandlerTransformer create(IMixinInfo mixin, SugarParameter parameter) {
        try {
            Class<? extends HandlerTransformer> clazz = MAP.get(parameter.sugar.desc);
            Constructor<? extends HandlerTransformer> ctor = clazz.getDeclaredConstructor(IMixinInfo.class, SugarParameter.class);
            return ctor.newInstance(mixin, parameter);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
