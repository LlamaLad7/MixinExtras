package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.google.gson.annotations.SerializedName;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool;
import com.llamalad7.mixinextras.expression.impl.serialization.SerializedTypeName;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;

@SerializedTypeName("bin")
public class BinaryExpression implements SimpleExpression {
    @SerializedName("l")
    public final Expression left;
    @SerializedName("op")
    public final Operator operator;
    @SerializedName("r")
    public final Expression right;

    public BinaryExpression(Expression left, Operator operator, Expression right) {
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

    @Override
    public boolean matches(FlowValue node, IdentifierPool pool, OutputSink sink) {
        return operator.matches(node.getInsn()) && inputsMatch(node, pool, sink, left, right);
    }

    public enum Operator {
        @SerializedName("*")
        MULT(Opcodes.IMUL, Opcodes.LMUL, Opcodes.FMUL, Opcodes.DMUL),
        @SerializedName("/")
        DIV(Opcodes.IDIV, Opcodes.LDIV, Opcodes.FDIV, Opcodes.DDIV),
        @SerializedName("%")
        MOD(Opcodes.IREM, Opcodes.LREM, Opcodes.FREM, Opcodes.DREM),
        @SerializedName("+")
        PLUS(Opcodes.IADD, Opcodes.LADD, Opcodes.FADD, Opcodes.DADD),
        @SerializedName("-")
        MINUS(Opcodes.ISUB, Opcodes.LSUB, Opcodes.FSUB, Opcodes.DSUB),
        @SerializedName("<<")
        SHL(Opcodes.ISHL, Opcodes.LSHL),
        @SerializedName(">>")
        SHR(Opcodes.ISHR, Opcodes.LSHR),
        @SerializedName(">>>")
        USHR(Opcodes.IUSHR, Opcodes.LUSHR),
        @SerializedName("&")
        BITWISE_AND(Opcodes.IAND, Opcodes.LAND),
        @SerializedName("^")
        BITWISE_XOR(Opcodes.IXOR, Opcodes.LXOR),
        @SerializedName("|")
        BITWISE_OR(Opcodes.IOR, Opcodes.LOR);

        private final int[] opcodes;

        Operator(int... opcodes) {
            this.opcodes = opcodes;
        }

        public boolean matches(AbstractInsnNode insn) {
            for (int opcode : opcodes) {
                if (opcode == insn.getOpcode()) {
                    return true;
                }
            }
            return false;
        }
    }
}
