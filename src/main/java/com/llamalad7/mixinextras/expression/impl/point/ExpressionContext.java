package com.llamalad7.mixinextras.expression.impl.point;

import com.llamalad7.mixinextras.expression.impl.ast.expressions.Expression;
import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class ExpressionContext {
    public final IdentifierPool pool;
    public final Expression.OutputSink sink;
    public final ClassNode classNode;
    public final MethodNode method;
    public final boolean isStatic;
    public final boolean allowIncompleteMethodArgs;

    public ExpressionContext(IdentifierPool pool, Expression.OutputSink sink, ClassNode classNode, MethodNode method, boolean allowIncompleteMethodArgs) {
        this.pool = pool;
        this.sink = sink;
        this.classNode = classNode;
        this.method = method;
        this.isStatic = (method.access & Opcodes.ACC_STATIC) != 0;
        this.allowIncompleteMethodArgs = allowIncompleteMethodArgs;
    }
}
