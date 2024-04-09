package com.llamalad7.mixinextras.expression.impl.point;

import com.llamalad7.mixinextras.expression.impl.ast.expressions.Expression;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool;
import com.llamalad7.mixinextras.injector.ModifyExpressionValueInjectionInfo;
import com.llamalad7.mixinextras.injector.ModifyReceiverInjectionInfo;
import com.llamalad7.mixinextras.injector.ModifyReturnValueInjectionInfo;
import com.llamalad7.mixinextras.injector.v2.WrapWithConditionInjectionInfo;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperationInjectionInfo;
import com.llamalad7.mixinextras.wrapper.WrapperInjectionInfo;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.struct.*;

public class ExpressionContext {
    public final IdentifierPool pool;
    private final Expression.OutputSink sink;
    public final ClassNode classNode;
    public final MethodNode method;
    public final Type type;
    public final boolean isStatic;
    public final boolean allowIncompleteListInputs;

    public ExpressionContext(IdentifierPool pool, Expression.OutputSink sink, ClassNode classNode, MethodNode method, Type type, boolean allowIncompleteListInputs) {
        this.pool = pool;
        this.sink = sink;
        this.classNode = classNode;
        this.method = method;
        this.type = type;
        this.isStatic = (method.access & Opcodes.ACC_STATIC) != 0;
        this.allowIncompleteListInputs = allowIncompleteListInputs;
    }

    public void capture(FlowValue node, Expression expression) {
        sink.capture(node, expression, this);
    }

    public void decorate(AbstractInsnNode insn, String key, Object value) {
        sink.decorate(insn, key, value);
    }

    public void decorateInjectorSpecific(AbstractInsnNode insn, String key, Object value) {
        sink.decorateInjectorSpecific(insn, key, value);
    }

    public enum Type {
        CUSTOM(null),
        INJECT(CallbackInjectionInfo.class),
        MODIFY_ARG(ModifyArgInjectionInfo.class),
        MODIFY_ARGS(ModifyArgsInjectionInfo.class),
        MODIFY_CONSTANT(ModifyConstantInjectionInfo.class),
        MODIFY_EXPRESSION_VALUE(ModifyExpressionValueInjectionInfo.class),
        MODIFY_RECEIVER(ModifyReceiverInjectionInfo.class),
        MODIFY_RETURN_VALUE(ModifyReturnValueInjectionInfo.class),
        MODIFY_VARIABLE(ModifyVariableInjectionInfo.class),
        REDIRECT(RedirectInjectionInfo.class),
        SLICE(null),
        WRAP_OPERATION(WrapOperationInjectionInfo.class),
        WRAP_WITH_CONDITION(WrapWithConditionInjectionInfo.class);

        private final Class<? extends InjectionInfo> infoClass;

        Type(Class<? extends InjectionInfo> infoClass) {
            this.infoClass = infoClass;
        }

        public static Type forContext(InjectionInfo info, boolean isInSlice) {
            if (isInSlice) {
                return SLICE;
            }
            while (info instanceof WrapperInjectionInfo) {
                info = ((WrapperInjectionInfo) info).getDelegate();
            }
            for (Type possible : values()) {
                if (info.getClass() == possible.infoClass) {
                    return possible;
                }
            }
            return CUSTOM;
        }
    }
}
