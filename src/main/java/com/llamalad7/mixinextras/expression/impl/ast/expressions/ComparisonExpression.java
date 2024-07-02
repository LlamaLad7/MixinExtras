package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ExpressionSource;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;
import com.llamalad7.mixinextras.expression.impl.utils.ComparisonInfo;
import com.llamalad7.mixinextras.expression.impl.utils.ComplexComparisonInfo;
import com.llamalad7.mixinextras.expression.impl.utils.ExpressionDecorations;
import com.llamalad7.mixinextras.expression.impl.utils.FlowDecorations;
import com.llamalad7.mixinextras.utils.Decorations;
import com.llamalad7.mixinextras.utils.TypeUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;

public class ComparisonExpression extends Expression {
    public final Expression left;
    public final Operator operator;
    public final Expression right;

    public ComparisonExpression(ExpressionSource src, Expression left, Operator operator, Expression right) {
        super(src);
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

    @Override
    public boolean matches(FlowValue node, ExpressionContext ctx) {
        return operator.matches(node, ctx) && inputsMatch(node, ctx, left, right);
    }

    @Override
    public void capture(FlowValue node, ExpressionContext ctx) {
        ctx.decorate(node.getInsn(), ExpressionDecorations.SIMPLE_EXPRESSION_TYPE, Type.BOOLEAN_TYPE);
        super.capture(node, ctx);
    }

    public enum Operator implements Opcodes {
        EQ(IF_ACMPEQ, IF_ICMPEQ, IF_ACMPNE, IF_ICMPNE, FCMPL, DCMPL),
        NE(IF_ACMPNE, IF_ICMPNE, IF_ACMPEQ, IF_ICMPEQ, FCMPL, DCMPL),
        LT(0, IF_ICMPLT, 0, IF_ICMPGE, FCMPG, DCMPG),
        LE(0, IF_ICMPLE, 0, IF_ICMPGT, FCMPG, DCMPG),
        GT(0, IF_ICMPGT, 0, IF_ICMPLE, FCMPL, DCMPL),
        GE(0, IF_ICMPGE, 0, IF_ICMPLT, FCMPL, DCMPL);

        private static final int WITH_ZERO_OFFSET = IF_ICMPEQ - IFEQ;

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

        public boolean matches(FlowValue node, ExpressionContext ctx) {
            AbstractInsnNode insn = node.getInsn();
            int opcode = insn.getOpcode();
            if (node.inputCount() != 2) {
                return false;
            }
            Type input;
            boolean isComplex = false;
            if (opcode == directObject || opcode == invertedObject) {
                input = TypeUtils.OBJECT_TYPE;
            } else if (opcode == directInt || opcode == invertedInt) {
                input = TypeUtils.getCommonSupertype(node.getInput(0).getType(), node.getInput(1).getType());
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
            ComparisonInfo info;
            if (isComplex) {
                int zeroDirect = directInt - WITH_ZERO_OFFSET;
                int zeroInverted = invertedInt - WITH_ZERO_OFFSET;
                JumpInsnNode jump = node.getDecoration(FlowDecorations.COMPLEX_COMPARISON_JUMP);
                if (jump == null || jump.getOpcode() != zeroDirect && jump.getOpcode() != zeroInverted) {
                    return false;
                }
                info = new ComplexComparisonInfo(opcode, insn, input, jump, jump.getOpcode() == zeroDirect);
            } else {
                info = new ComparisonInfo(opcode, insn, input, opcode == directObject || opcode == directInt);
            }
            info.attach(
                    (k, v) -> ctx.decorate(insn, k, v),
                    (k, v) -> ctx.decorateInjectorSpecific(insn, k, v)
            );
            return true;
        }
    }
}
