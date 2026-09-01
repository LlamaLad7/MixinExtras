package com.llamalad7.mixinextras.platform.neoforge;

import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import com.llamalad7.mixinextras.utils.ResourceUtils;
import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.api.IModuleLayerManager;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class MixinExtrasConfigPlugin implements IMixinConfigPlugin {
    @Override
    public void onLoad(String mixinPackage) {
        try {
            Class.forName("cpw.mods.modlauncher.Launcher");
            MixinExtrasBootstrap.init(new ModLauncherConfigsFinder());
            return;
        } catch (ClassNotFoundException ignored) {}
        MixinExtrasBootstrap.init();
    }
    
    private static class ModLauncherConfigsFinder implements ResourceUtils.ConfigsFinder {
        @Override
        public ClassLoader getClassLoader() {
            return Launcher.INSTANCE.findLayerManager()
                    // Get the game layer
                    .flatMap(layerManager -> layerManager.getLayer(IModuleLayerManager.Layer.GAME))
                    // Get "minecraft" module or "neoforge" module off the game layer
                    .flatMap(layer -> layer.findModule("minecraft").or(() -> layer.findModule("neoforge")))
                    .map(Module::getClassLoader)
                    .orElseThrow(() -> new IllegalStateException("Cannot find a ClassLoader for the game layer!"));
        }
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
