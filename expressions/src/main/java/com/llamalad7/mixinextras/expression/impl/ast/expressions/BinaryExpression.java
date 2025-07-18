package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ExpressionSource;
import com.llamalad7.mixinextras.expression.impl.utils.ExpressionDecorations;
import com.llamalad7.mixinextras.expression.impl.utils.ExpressionUtil;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.flow.postprocessing.StringConcatInfo;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;
import com.llamalad7.mixinextras.expression.impl.utils.FlowDecorations;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;

public class BinaryExpression extends SimpleExpression {
    public final Expression left;
    public final Operator operator;
    public final Expression right;

    public BinaryExpression(ExpressionSource src, Expression left, Operator operator, Expression right) {
        super(src);
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

    @Override
    protected boolean matchesImpl(FlowValue node, ExpressionContext ctx) {
        if (operator.matches(node.getInsn()) && inputsMatch(node, ctx, left, right)) {
            return true;
        }
        StringConcatInfo concat = node.getDecoration(FlowDecorations.STRING_CONCAT_INFO);
        if (operator != Operator.PLUS || concat == null) {
            return false;
        }
        ctx.reportPartialMatch(node, this);
        if (node == concat.toStringCall) {
            node = node.getInput(0);
        }
        if (!right.matches(node.getInput(1), ctx)) {
            return false;
        }
        if (concat.isFirstConcat) {
            return left.matches(concat.initialComponent, ctx);
        }
        Expression innerLeft = ExpressionUtil.skipCapturesDown(left);
        if (innerLeft instanceof WildcardExpression) {
            if (left instanceof CapturingExpression) {
                // The wildcard will match the concatenation to the left, but won't decorate it as a concat, so we do it
                // ourselves.
                checkSupportsStringConcat(ctx.type);
                ctx.decorateInjectorSpecific(node.getInput(0).getInsn(), ExpressionDecorations.IS_STRING_CONCAT_EXPRESSION, true);
            }
            // Do the match:
            return left.matches(node.getInput(0), ctx);
        }
        if (innerLeft instanceof BinaryExpression && ((BinaryExpression) innerLeft).operator == Operator.PLUS) {
            // Continue matching the concat chain.
            return left.matches(node.getInput(0), ctx);
        }
        return false;
    }

    @Override
    public void capture(FlowValue node, ExpressionContext ctx) {
        StringConcatInfo concat = node.getDecoration(FlowDecorations.STRING_CONCAT_INFO);
        if (concat == null) {
            super.capture(node, ctx);
            return;
        }
        checkSupportsStringConcat(ctx.type);
        if (concat.isBuilder) {
            ctx.decorateInjectorSpecific(node.getInsn(), ExpressionDecorations.IS_STRING_CONCAT_EXPRESSION, true);
        }
        super.capture(node, ctx);
    }

    private void checkSupportsStringConcat(ExpressionContext.Type type) {
        switch (type) {
            case SLICE:
            case INJECT:
            case MODIFY_VARIABLE:
                // Tolerate, they don't care about their target instruction.
                return;
            case MODIFY_EXPRESSION_VALUE:
                // Supported.
                return;
        }
        throw new UnsupportedOperationException(
                String.format(
                        "Expression context type %s does not support string concat!",
                        type
                )
        );
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
