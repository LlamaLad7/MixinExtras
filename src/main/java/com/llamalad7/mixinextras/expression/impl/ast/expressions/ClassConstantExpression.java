package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ast.identifiers.Identifier;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;

public class ClassConstantExpression implements SimpleExpression {
    public final Identifier type;

    public ClassConstantExpression(Identifier type) {
        this.type = type;
    }

    @Override
    public boolean matches(FlowValue node, ExpressionContext ctx) {
        return isValid(node.getInsn()) && type.matches(ctx.getPool(), node.getInsn(), Identifier.Role.TYPE);
    }

    private boolean isValid(AbstractInsnNode insn) {
        if (insn instanceof LdcInsnNode && ((LdcInsnNode) insn).cst instanceof Type) {
            return true;
        }
        if (insn.getOpcode() != Opcodes.GETSTATIC) {
            return false;
        }
        FieldInsnNode get = (FieldInsnNode) insn;
        if (!get.name.equals("TYPE") || !get.desc.equals(Type.getDescriptor(Class.class))) {
            return false;
        }
        switch (get.owner) {
            case "java/lang/Boolean":
            case "java/lang/Character":
            case "java/lang/Byte":
            case "java/lang/Short":
            case "java/lang/Integer":
            case "java/lang/Float":
            case "java/lang/Long":
            case "java/lang/Double":
                return true;
        }
        return false;
    }
}
