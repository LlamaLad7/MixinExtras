package com.llamalad7.mixinextras.expression.impl.pool;

import com.llamalad7.mixinextras.utils.CompatibilityHelper;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.spongepowered.asm.mixin.injection.selectors.MatchResult;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;

class FieldDef implements SimpleMemberDefinition {
    private final MemberInfo memberInfo;

    public FieldDef(String field, InjectionInfo info) {
        this.memberInfo = CompatibilityHelper.parseMemberInfo(field, info);
    }

    @Override
    public boolean matches(AbstractInsnNode insn) {
        if (!(insn instanceof FieldInsnNode)) {
            return false;
        }
        FieldInsnNode fieldNode = (FieldInsnNode) insn;
        return memberInfo.matches(fieldNode.owner, fieldNode.name, fieldNode.desc) == MatchResult.EXACT_MATCH;
    }
}
