package com.llamalad7.mixinextras.expression.impl.pool;

import com.llamalad7.mixinextras.utils.CompatibilityHelper;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.spongepowered.asm.mixin.injection.selectors.MatchResult;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;

class MethodDef implements MemberDefinition {
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
}
