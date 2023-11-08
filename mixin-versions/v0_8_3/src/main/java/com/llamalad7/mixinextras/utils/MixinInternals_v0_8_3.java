package com.llamalad7.mixinextras.utils;

import com.llamalad7.mixinextras.utils.InternalField;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class MixinInternals_v0_8_3 {
    private static final InternalField<InjectionInfo, List<?>> INJECTION_INFO_SELECTED_TARGETS
            = InternalField.of(InjectionInfo.class, "targets");
    private static final InternalField<Object, MethodNode> SELECTED_TARGET_METHOD
            = InternalField.of("org.spongepowered.asm.mixin.injection.struct.InjectionInfo$SelectedTarget", "method");

    public static Collection<MethodNode> getTargets(InjectionInfo info) {
        return INJECTION_INFO_SELECTED_TARGETS.get(info).stream().map(SELECTED_TARGET_METHOD::get).collect(Collectors.toList());
    }
}
