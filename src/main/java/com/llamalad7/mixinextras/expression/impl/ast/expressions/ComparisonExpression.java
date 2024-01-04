package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool;
import com.llamalad7.mixinextras.expression.impl.utils.ComparisonInfo;
import com.llamalad7.mixinextras.expression.impl.utils.ComplexComparisonInfo;
import com.llamalad7.mixinextras.utils.ASMUtils;
import com.llamalad7.mixinextras.utils.Decorations;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;

public class ComparisonExpression implements Expression {
    private static final long serialVersionUID = -5524038763761980419L;

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

    private boolean matchesImpl(FlowValue node, IdentifierPool pool, OutputSink sink, boolean isWithZero, boolean isWithNull) {
        if (!operator.matches(node, sink, isWithZero, isWithNull)) {
            return false;
        }
        if ((isWithZero || isWithNull) && inputsMatch(node, pool, sink, left)) {
            return true;
        }
        return inputsMatch(node, pool, sink, left, right);
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
            int opcode = node.getInsn().getOpcode();
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
                if (isComplexComparison(node.getInput(0).getInsn())) {
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
                input = ASMUtils.OBJECT_TYPE;
            } else if (opcode == directInt || opcode == invertedInt) {
                input = Type.INT_TYPE;
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
            if (isWithZero && input != Type.INT_TYPE || isWithNull && input != ASMUtils.OBJECT_TYPE) {
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
                info = new ComplexComparisonInfo(opcode, node.getInsn(), input, jump, jump.getOpcode() == zeroDirect);
            } else {
                info = new ComparisonInfo(opcode, node.getInsn(), input, needsExpanding, opcode == directObject || opcode == directInt);
            }
            sink.decorateInjectorSpecific(node.getInsn(), Decorations.COMPARISON_INFO, info);
            return true;
        }

        private boolean isComplexComparison(AbstractInsnNode insn) {
            if (insn == null) {
                return false;
            }
            switch (insn.getOpcode()) {
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
