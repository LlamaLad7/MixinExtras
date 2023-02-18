package com.llamalad7.mixinextras.sugar.impl;

import com.llamalad7.mixinextras.sugar.passback.impl.PassBackInfo;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.util.asm.MethodNodeEx;

import java.util.HashMap;
import java.util.Map;

class SugarInfo {
    private final Map<String, PassBackInfo> passBackMap = new HashMap<>();

    void addPassBack(MethodNode handler, PassBackInfo info) {
        passBackMap.put(handler.name + handler.desc, info);
    }

    PassBackInfo getPassBack(MethodNode handler) {
        return passBackMap.get(MethodNodeEx.getName(handler) + handler.desc);
    }
}
