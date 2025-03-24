package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ExpressionSource;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;
import com.llamalad7.mixinextras.expression.impl.utils.ComparisonInfo;
import com.llamalad7.mixinextras.expression.impl.utils.ComplexComparisonInfo;
import com.llamalad7.mixinextras.expression.impl.utils.ExpressionDecorations;
import com.llamalad7.mixinextras.expression.impl.utils.FlowDecorations;
import com.llamalad7.mixinextras.expression.impl.utils.ExpressionASMUtils;
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
        EQ(IF_ACMPEQ, IF_ICMPEQ, IF_ACMPNE, IF_ICMPNE, FCMPL, DCMPL, FCMPG, DCMPG),
        NE(IF_ACMPNE, IF_ICMPNE, IF_ACMPEQ, IF_ICMPEQ, FCMPL, DCMPL, FCMPG, DCMPG),
        LT(0, IF_ICMPLT, 0, IF_ICMPGE, FCMPG, DCMPG),
        LE(0, IF_ICMPLE, 0, IF_ICMPGT, FCMPG, DCMPG),
        GT(0, IF_ICMPGT, 0, IF_ICMPLE, FCMPL, DCMPL),
        GE(0, IF_ICMPGE, 0, IF_ICMPLT, FCMPL, DCMPL);

        private static final int WITH_ZERO_OFFSET = IF_ICMPEQ - IFEQ;

        private final int directObject;
        private final int directInt;
        private final int invertedObject;
        private final int invertedInt;
        private final int fcmp1;
        private final int dcmp1;
        private final int fcmp2;
        private final int dcmp2;

        Operator(int directObject, int directInt, int invertedObject, int invertedInt, int fcmp1, int dcmp1, int fcmp2, int dcmp2) {
            this.directObject = directObject;
            this.directInt = directInt;
            this.invertedObject = invertedObject;
            this.invertedInt = invertedInt;
            this.fcmp1 = fcmp1;
            this.dcmp1 = dcmp1;
            this.fcmp2 = fcmp2;
            this.dcmp2 = dcmp2;
        }

        Operator(int directObject, int directInt, int invertedObject, int invertedInt, int fcmp, int dcmp) {
            this(directObject, directInt, invertedObject, invertedInt, fcmp, dcmp, fcmp, dcmp);
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
                input = ExpressionASMUtils.OBJECT_TYPE;
            } else if (opcode == directInt || opcode == invertedInt) {
                input = ExpressionASMUtils.getCommonSupertype(null, node.getInput(0).getType(), node.getInput(1).getType());
            } else if (opcode == LCMP) {
                input = Type.LONG_TYPE;
                isComplex = true;
            } else if (opcode == fcmp1 || opcode == fcmp2) {
                input = Type.FLOAT_TYPE;
                isComplex = true;
            } else if (opcode == dcmp1 || opcode == dcmp2) {
                input = Type.DOUBLE_TYPE;
                isComplex = true;
            } else {
                return false;
            }
            ComparisonInfo info;
            if (isComplex) {
                int zeroDirect = directInt - WITH_ZERO_OFFSET;
                int zeroInverted = invertedInt - WITH_ZERO_OFFSET;
                FlowValue jumpNode = node.getDecoration(FlowDecorations.COMPLEX_COMPARISON_JUMP);
                JumpInsnNode jump = (JumpInsnNode) jumpNode.getInsn();
                if (jump == null || jump.getOpcode() != zeroDirect && jump.getOpcode() != zeroInverted) {
                    return false;
                }
                info = new ComplexComparisonInfo(opcode, node, input, jumpNode, jump.getOpcode() == zeroDirect);
            } else {
                info = new ComparisonInfo(opcode, node, input, opcode == directObject || opcode == directInt);
            }
            info.attach(
                    (k, v) -> ctx.decorate(insn, k, v),
                    (k, v) -> ctx.decorateInjectorSpecific(insn, k, v)
            );
            return true;
        }
    }
}
