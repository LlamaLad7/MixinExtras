package com.llamalad7.mixinextras.expression.impl.pool;

import com.llamalad7.mixinextras.utils.CompatibilityHelper;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.spongepowered.asm.mixin.injection.selectors.MatchResult;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;

class MethodDef implements SimpleMemberDefinition {
    private final MemberInfo memberInfo;

    public MethodDef(String method, InjectionInfo info) {
        this.memberInfo = CompatibilityHelper.parseMemberInfo(method, info);
    }

    @Override
    public boolean matches(AbstractInsnNode insn) {
        if (!(insn instanceof MethodInsnNode)) {
            return false;
        }
        MethodInsnNode methodNode = (MethodInsnNode) insn;
        return memberInfo.matches(methodNode.owner, methodNode.name, methodNode.desc) == MatchResult.EXACT_MATCH;
    }

    @Override
    public boolean matches(Handle handle) {
        switch (handle.getTag()) {
            case Opcodes.H_INVOKEVIRTUAL:
            case Opcodes.H_INVOKESTATIC:
            case Opcodes.H_INVOKESPECIAL:
            case Opcodes.H_INVOKEINTERFACE:
                return memberInfo.matches(handle.getOwner(), handle.getName(), handle.getDesc()) == MatchResult.EXACT_MATCH;
        }
        return false;
    }
}
