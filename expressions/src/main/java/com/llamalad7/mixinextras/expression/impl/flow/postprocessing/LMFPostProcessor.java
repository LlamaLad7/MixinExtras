package com.llamalad7.mixinextras.expression.impl.flow.postprocessing;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.utils.ExpressionASMUtils;
import com.llamalad7.mixinextras.expression.impl.utils.FlowDecorations;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;

public class LMFPostProcessor implements FlowPostProcessor {
    private final Type currentType;

    public LMFPostProcessor(ClassNode classNode) {
        currentType = Type.getObjectType(classNode.name);
    }

    @Override
    public void process(FlowValue node, OutputSink sink) {
        if (node.getInsn().getOpcode() != Opcodes.INVOKEDYNAMIC) {
            return;
        }
        InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) node.getInsn();
        if (!indy.bsm.equals(ExpressionASMUtils.LMF_HANDLE) && !indy.bsm.equals(ExpressionASMUtils.ALT_LMF_HANDLE)) {
            return;
        }
        Handle impl = (Handle) indy.bsmArgs[1];
        LMFInfo.Type type = getType(node, impl);
        if (type == null) {
            return;
        }
        node.decorate(FlowDecorations.LMF_INFO, new LMFInfo(impl, type));
    }

    private LMFInfo.Type getType(FlowValue node, Handle impl) {
        boolean bound = node.inputCount() != 0;
        switch (impl.getTag()) {
            case Opcodes.H_NEWINVOKESPECIAL:
                return bound ? null : LMFInfo.Type.INSTANTIATION;
            case Opcodes.H_INVOKESPECIAL:
                if (!impl.getOwner().equals(currentType.getInternalName())) {
                    return null;
                }
            case Opcodes.H_INVOKEVIRTUAL:
            case Opcodes.H_INVOKEINTERFACE:
                return bound ? LMFInfo.Type.BOUND_METHOD : LMFInfo.Type.FREE_METHOD;
            case Opcodes.H_INVOKESTATIC:
                return LMFInfo.Type.FREE_METHOD;
        }
        return null;
    }
}
