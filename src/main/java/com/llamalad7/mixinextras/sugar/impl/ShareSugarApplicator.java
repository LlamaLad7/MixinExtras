package com.llamalad7.mixinextras.sugar.impl;

import com.llamalad7.mixinextras.injector.StackExtension;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes;
import org.spongepowered.asm.mixin.injection.struct.Target;

class ShareSugarApplicator extends SugarApplicator {
    ShareSugarApplicator(InjectionInfo info, SugarParameter parameter) {
        super(info, parameter);
    }

    @Override
    void validate(Target target, InjectionNodes.InjectionNode node) {
    }

    @Override
    void prepare(Target target, InjectionNodes.InjectionNode node) {
    }

    @Override
    void inject(Target target, InjectionNodes.InjectionNode node, StackExtension stack) {
        ShareInfo info = ShareInfo.getOrCreate(target, sugar, paramType, mixin, stack);
        stack.extra(1);
        target.insns.insertBefore(node.getCurrentTarget(), info.load());
    }

}
