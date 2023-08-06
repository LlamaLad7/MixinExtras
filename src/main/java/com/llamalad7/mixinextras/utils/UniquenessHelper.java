package com.llamalad7.mixinextras.utils;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class UniquenessHelper {
    public static String getUniqueMethodName(ClassNode classNode, String name) {
        for (int counter = classNode.methods.size(); ; counter++) {
            String candidate = name + '$' + counter;
            boolean isValid = true;
            for (MethodNode methodNode : classNode.methods) {
                if (methodNode.name.equals(candidate)) {
                    isValid = false;
                    break;
                }
            }
            if (isValid) {
                return candidate;
            }
        }
    }
}
