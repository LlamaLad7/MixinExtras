package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ExpressionSource;
import com.llamalad7.mixinextras.expression.impl.ast.identifiers.TypeIdentifier;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;

public class ClassConstantExpression extends SimpleExpression {
    public final TypeIdentifier type;

    public ClassConstantExpression(ExpressionSource src, TypeIdentifier type) {
        super(src);
        this.type = type;
    }

    @Override
    public boolean matches(FlowValue node, ExpressionContext ctx) {
        Type cstType = getConstantType(node.getInsn());
        return cstType != null && type.matches(ctx.pool, cstType);
    }

    private Type getConstantType(AbstractInsnNode insn) {
        if (insn instanceof LdcInsnNode) {
            Object cst = ((LdcInsnNode) insn).cst;
            return cst instanceof Type ? (Type) cst : null;
        }
        if (insn.getOpcode() != Opcodes.GETSTATIC) {
            return null;
        }
        FieldInsnNode get = (FieldInsnNode) insn;
        if (!get.name.equals("TYPE") || !get.desc.equals(Type.getDescriptor(Class.class))) {
            return null;
        }
        switch (get.owner) {
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
}
