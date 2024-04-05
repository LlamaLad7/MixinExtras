package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ExpressionSource;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;
import com.llamalad7.mixinextras.utils.ASMUtils;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;

public abstract class MethodReferenceExpression extends SimpleExpression {
    public MethodReferenceExpression(ExpressionSource src) {
        super(src);
    }

    public abstract boolean matches(FlowValue node, Handle impl, ExpressionContext ctx);

    @Override
    public boolean matches(FlowValue node, ExpressionContext ctx) {
        if (node.getInsn().getOpcode() != Opcodes.INVOKEDYNAMIC) {
            return false;
        }
        InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) node.getInsn();
        if (!indy.bsm.equals(ASMUtils.LMF_HANDLE) && !indy.bsm.equals(ASMUtils.ALT_LMF_HANDLE)) {
            return false;
        }
        Handle impl = (Handle) indy.bsmArgs[1];
        return matches(node, impl, ctx);
    }
}
