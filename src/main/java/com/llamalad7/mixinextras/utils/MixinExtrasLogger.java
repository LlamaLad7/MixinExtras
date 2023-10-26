package com.llamalad7.mixinextras.utils;

import org.spongepowered.asm.service.IMixinService;
import org.spongepowered.asm.service.MixinService;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public interface MixinExtrasLogger {
    void warn(String message, Object... args);

    void info(String message, Object... args);

    void debug(String message, Object... args);

    void error(String message, Throwable t);

    static MixinExtrasLogger get(String name) {
        Object impl;
        try {
            IMixinService service = MixinService.getService();
            Method getLogger = service.getClass().getMethod("getLogger", String.class);
            impl = getLogger.invoke(service, "MixinExtras|" + name);
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e1) {
            try {
                impl = Class.forName("org.apache.logging.log4j.LogManager").getMethod("getLogger", String.class).invoke(null, name);
            } catch (ClassNotFoundException | InvocationTargetException | IllegalAccessException |
                     NoSuchMethodException e2) {
                RuntimeException e = new IllegalStateException("Could not get logger! Please inform LlamaLad7!");
                e.addSuppressed(e1);
                e.addSuppressed(e2);
                throw e;
            }
        }
        Object finalImpl = impl;
        return (MixinExtrasLogger) Proxy.newProxyInstance(MixinExtrasLogger.class.getClassLoader(), new Class[]{MixinExtrasLogger.class}, (proxy, method, args) -> {
            return finalImpl.getClass().getMethod(method.getName(), method.getParameterTypes()).invoke(finalImpl, args);
        });
    }
}
