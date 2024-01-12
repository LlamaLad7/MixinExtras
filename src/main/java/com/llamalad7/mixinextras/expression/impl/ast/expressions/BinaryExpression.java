package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool;
import com.llamalad7.mixinextras.expression.impl.serialization.ExpressionReader;
import com.llamalad7.mixinextras.expression.impl.serialization.ExpressionWriter;
import com.llamalad7.mixinextras.expression.impl.serialization.SerializedExpressionId;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;

import java.io.IOException;

@SerializedExpressionId("bin")
public class BinaryExpression implements SimpleExpression {
    public final Expression left;
    public final Operator operator;
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

    @Override
    public void write(ExpressionWriter writer) throws IOException {
        writer.writeExpression(left);
        writer.writeEnum(operator);
        writer.writeExpression(right);
    }

    public static Expression read(ExpressionReader reader) throws IOException {
        return new BinaryExpression(reader.readExpression(), reader.readEnum(Operator::valueOf), reader.readExpression());
    }

    public enum Operator {
        MULT(Opcodes.IMUL, Opcodes.LMUL, Opcodes.FMUL, Opcodes.DMUL),
        DIV(Opcodes.IDIV, Opcodes.LDIV, Opcodes.FDIV, Opcodes.DDIV),
        MOD(Opcodes.IREM, Opcodes.LREM, Opcodes.FREM, Opcodes.DREM),
        PLUS(Opcodes.IADD, Opcodes.LADD, Opcodes.FADD, Opcodes.DADD),
        MINUS(Opcodes.ISUB, Opcodes.LSUB, Opcodes.FSUB, Opcodes.DSUB),
        SHL(Opcodes.ISHL, Opcodes.LSHL),
        SHR(Opcodes.ISHR, Opcodes.LSHR),
        USHR(Opcodes.IUSHR, Opcodes.LUSHR),
        BITWISE_AND(Opcodes.IAND, Opcodes.LAND),
        BITWISE_XOR(Opcodes.IXOR, Opcodes.LXOR),
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
