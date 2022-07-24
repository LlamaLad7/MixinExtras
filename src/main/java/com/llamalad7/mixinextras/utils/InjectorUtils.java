package com.llamalad7.mixinextras.utils;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes;

public class InjectorUtils {
    public static boolean isVirtualRedirect(InjectionNodes.InjectionNode node) {
        return node.isReplaced() && node.hasDecoration("redirector") && node.getCurrentTarget().getOpcode() != Opcodes.INVOKESTATIC;
    }
}
