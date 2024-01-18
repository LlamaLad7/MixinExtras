package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool;
import com.llamalad7.mixinextras.expression.impl.serialization.ExpressionReader;
import com.llamalad7.mixinextras.expression.impl.serialization.ExpressionWriter;
import com.llamalad7.mixinextras.expression.impl.serialization.SerializedExpressionId;
import com.llamalad7.mixinextras.expression.impl.utils.ComparisonInfo;
import com.llamalad7.mixinextras.expression.impl.utils.ComplexComparisonInfo;
import com.llamalad7.mixinextras.utils.TypeUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;

import java.io.IOException;

@SerializedExpressionId("cmp")
public class ComparisonExpression implements Expression {
    public final Expression left;
    public final Operator operator;
    public final Expression right;
    private final boolean isWithZero;
    private final boolean isWithNull;
    private final boolean isWildcard;

    public ComparisonExpression(Expression left, Operator operator, Expression right) {
        this.left = left;
        this.operator = operator;
        this.right = right;
        this.isWithZero = right instanceof IntLiteralExpression && ((IntLiteralExpression) right).value == 0;
        this.isWithNull = right instanceof NullLiteralExpression;
        this.isWildcard = right instanceof WildcardExpression;
    }

    @Override
    public boolean matches(FlowValue node, IdentifierPool pool, OutputSink sink) {
        boolean matches = matchesImpl(node, pool, sink, false, false);
        if (isWithZero || isWildcard) {
            matches = matches || matchesImpl(node, pool, sink, true, false);
        }
        if (isWithNull || isWildcard) {
            matches = matches || matchesImpl(node, pool, sink, false, true);
        }
        return matches;
    }

    @Override
    public void write(ExpressionWriter writer) throws IOException {
        writer.writeExpression(left);
        writer.writeEnum(operator);
        writer.writeExpression(right);
    }

    private boolean matchesImpl(FlowValue node, IdentifierPool pool, OutputSink sink, boolean isWithZero, boolean isWithNull) {
        if (!operator.matches(node, sink, isWithZero, isWithNull)) {
            return false;
        }
        if ((isWithZero || isWithNull) && inputsMatch(node, pool, sink, left)) {
            return true;
        }
        return inputsMatch(node, pool, sink, left, right);
    }

    public static Expression read(ExpressionReader reader) throws IOException {
        return new ComparisonExpression(reader.readExpression(), reader.readEnum(Operator::valueOf), reader.readExpression());
    }

    public enum Operator implements Opcodes {
        EQ(IF_ACMPEQ, IF_ICMPEQ, IF_ACMPNE, IF_ICMPNE, FCMPL, DCMPL),
        NE(IF_ACMPNE, IF_ICMPNE, IF_ACMPEQ, IF_ICMPEQ, FCMPL, DCMPL),
        LT(0, IF_ICMPLT, 0, IF_ICMPGE, FCMPG, DCMPG),
        LE(0, IF_ICMPLE, 0, IF_ICMPGT, FCMPG, DCMPG),
        GT(0, IF_ICMPGT, 0, IF_ICMPLE, FCMPL, DCMPL),
        GE(0, IF_ICMPGE, 0, IF_ICMPLT, FCMPL, DCMPL);

        private static final int WITH_ZERO_OFFSET = IF_ICMPEQ - IFEQ;
        private static final int WITH_NULL_OFFSET = IFNULL - IF_ACMPEQ;

        private final int directObject;
        private final int directInt;
        private final int invertedObject;
        private final int invertedInt;
        private final int fcmp;
        private final int dcmp;

        Operator(int directObject, int directInt, int invertedObject, int invertedInt, int fcmp, int dcmp) {
            this.directObject = directObject;
            this.directInt = directInt;
            this.invertedObject = invertedObject;
            this.invertedInt = invertedInt;
            this.fcmp = fcmp;
            this.dcmp = dcmp;
        }

        public boolean matches(FlowValue node, OutputSink sink, boolean isWithZero, boolean isWithNull) {
            AbstractInsnNode insn = node.getInsn();
            int opcode = insn.getOpcode();
            boolean needsExpanding = false;
            if (node.inputCount() == 1) {
                needsExpanding = true;
                if (isWithZero) {
                    opcode += WITH_ZERO_OFFSET;
                } else if (isWithNull) {
                    opcode -= WITH_NULL_OFFSET;
                } else {
                    return false;
                }
                if (isComplexComparison(node.getInput(0))) {
                    // Complex comparisons are similar to `lcmp(a, b) == 0`, for example, but the comparison with zero
                    // should not be considered.
                    return false;
                }
            } else if (node.inputCount() != 2) {
                return false;
            }
            Type input;
            boolean isComplex = false;
            if (opcode == directObject || opcode == invertedObject) {
                input = TypeUtils.OBJECT_TYPE;
            } else if (opcode == directInt || opcode == invertedInt) {
                if (node.inputCount() == 1) {
                    input = node.getInput(0).getType();
                } else {
                    input = TypeUtils.getCommonSupertype(node.getInput(0).getType(), node.getInput(1).getType());
                }
            } else if (opcode == LCMP) {
                input = Type.LONG_TYPE;
                isComplex = true;
            } else if (opcode == fcmp) {
                input = Type.FLOAT_TYPE;
                isComplex = true;
            } else if (opcode == dcmp) {
                input = Type.DOUBLE_TYPE;
                isComplex = true;
            } else {
                return false;
            }
            if (isWithZero && input != Type.INT_TYPE || isWithNull && input != TypeUtils.OBJECT_TYPE) {
                return false;
            }
            ComparisonInfo info;
            if (isComplex) {
                if (node.getNext().size() != 1) {
                    return false;
                }
                int zeroDirect = directInt - WITH_ZERO_OFFSET;
                int zeroInverted = invertedInt - WITH_ZERO_OFFSET;
                AbstractInsnNode next = node.getNext().iterator().next().getLeft().getInsn();
                if (next.getOpcode() != zeroDirect && next.getOpcode() != zeroInverted) {
                    return false;
                }
                JumpInsnNode jump = (JumpInsnNode) next;
                info = new ComplexComparisonInfo(opcode, insn, input, jump, jump.getOpcode() == zeroDirect);
            } else {
                info = new ComparisonInfo(opcode, insn, input, needsExpanding, opcode == directObject || opcode == directInt);
            }
            info.attach(
                    (k, v) -> sink.decorate(insn, k, v),
                    (k, v) -> sink.decorateInjectorSpecific(insn, k, v)
            );
            return true;
        }

        private boolean isComplexComparison(FlowValue node) {
            if (node.isComplex()) {
                return false;
            }
            switch (node.getInsn().getOpcode()) {
                case LCMP:
                case FCMPL:
                case FCMPG:
                case DCMPL:
                case DCMPG:
                    return true;
            }
            return false;
        }
    }
}
