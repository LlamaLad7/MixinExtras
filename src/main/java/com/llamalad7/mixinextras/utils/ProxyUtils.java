package com.llamalad7.mixinextras.utils;

import org.apache.commons.lang3.ClassUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class ProxyUtils {
    @SuppressWarnings("unchecked")
    public static <T> T getProxy(Object impl, Class<T> interfaceClass) {
        if (interfaceClass.isInstance(impl)) {
            return interfaceClass.cast(impl);
        }
        String simpleName = interfaceClass.getSimpleName();
        if (ClassUtils.getAllInterfaces(impl.getClass()).stream().anyMatch(it -> it.getName().endsWith('.' + simpleName))) {
            return (T) Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class[]{interfaceClass}, (proxy, method, args) -> {
                Method original = impl.getClass().getMethod(method.getName(), method.getParameterTypes());
                original.setAccessible(true);
                return original.invoke(impl, args);
            });
        }
        throw new UnsupportedOperationException(String.format("Cannot get a %s instance from %s", simpleName, impl));
    }
}
