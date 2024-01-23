package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ast.identifiers.TypeIdentifier;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;
import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.util.List;

public class InstantiationExpression implements Expression {
    public final TypeIdentifier type;
    public final List<Expression> arguments;

    public InstantiationExpression(TypeIdentifier type, List<Expression> arguments) {
        this.type = type;
        this.arguments = arguments;
    }

    @Override
    public boolean matches(FlowValue node, ExpressionContext ctx) {
        AbstractInsnNode insn = node.getInsn();
        if (insn.getOpcode() != Opcodes.NEW) {
            return false;
        }
        Type newType = Type.getObjectType(((TypeInsnNode) insn).desc);
        if (!type.matches(ctx.getPool(), newType)) {
            return false;
        }
        for (Pair<FlowValue, Integer> next : node.getNext()) {
            if (next.getRight() != 0) continue;
            FlowValue nextValue = next.getLeft();
            AbstractInsnNode nextInsn = nextValue.getInsn();
            if (
                    nextInsn.getOpcode() == Opcodes.INVOKESPECIAL
                            && ((MethodInsnNode) nextInsn).name.equals("<init>")
                            && nextValue.getInput(0) == node
                            && inputsMatch(1, nextValue, ctx, arguments.toArray(new Expression[0]))
            ) {
                return true;
            }
        }
        return false;
    }
}
