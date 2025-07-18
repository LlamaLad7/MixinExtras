package com.llamalad7.mixinextras.expression.impl.flow;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodInsnNode;

public class Boxing {
    static boolean isBoxing(MethodInsnNode call) {
        String unboxingMethod = getUnboxingMethod(call.owner);
        if (unboxingMethod == null) {
            return false;
        }
        Type[] paramTypes = Type.getArgumentTypes(call.desc);
        Type unboxedType = getUnboxedType(Type.getObjectType(call.owner));
        return call.name.equals(unboxingMethod) ||
                call.name.equals("valueOf") && paramTypes.length == 1 && paramTypes[0].equals(unboxedType);
    }

    public static Type getUnboxedType(Type boxedType) {
        if (boxedType.getSort() != Type.OBJECT) {
            return null;
        }
        switch (boxedType.getInternalName()) {
            case "java/lang/Boolean":
                return Type.BOOLEAN_TYPE;
            case "java/lang/Character":
                return Type.CHAR_TYPE;
            case "java/lang/Byte":
                return Type.BYTE_TYPE;
            case "java/lang/Short":
                return Type.SHORT_TYPE;
            case "java/lang/Integer":
                return Type.INT_TYPE;
            case "java/lang/Float":
                return Type.FLOAT_TYPE;
            case "java/lang/Long":
                return Type.LONG_TYPE;
            case "java/lang/Double":
                return Type.DOUBLE_TYPE;
        }
        return null;
    }

    private static String getUnboxingMethod(String owner) {
        switch (owner) {
            case "java/lang/Boolean":
                return "booleanValue";
            case "java/lang/Character":
                return "charValue";
            case "java/lang/Byte":
                return "byteValue";
            case "java/lang/Short":
                return "shortValue";
            case "java/lang/Integer":
                return "intValue";
            case "java/lang/Float":
                return "floatValue";
            case "java/lang/Long":
                return "longValue";
            case "java/lang/Double":
                return "doubleValue";
        }
        return null;
    }
}
