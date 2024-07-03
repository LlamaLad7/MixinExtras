package com.llamalad7.mixinextras.expression.impl.ast.expressions;

import com.llamalad7.mixinextras.expression.impl.ExpressionSource;
import com.llamalad7.mixinextras.expression.impl.ast.identifiers.MemberIdentifier;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.flow.postprocessing.LMFInfo;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;
import com.llamalad7.mixinextras.expression.impl.utils.FlowDecorations;

public class FreeMethodReferenceExpression extends SimpleExpression {
    public final MemberIdentifier name;

    public FreeMethodReferenceExpression(ExpressionSource src, MemberIdentifier name) {
        super(src);
        this.name = name;
    }

    @Override
    public boolean matches(FlowValue node, ExpressionContext ctx) {
        LMFInfo info = node.getDecoration(FlowDecorations.LMF_INFO);
        if (info == null || info.type != LMFInfo.Type.FREE_METHOD) {
            return false;
        }
        return name.matches(ctx.pool, info.impl);
    }
}
