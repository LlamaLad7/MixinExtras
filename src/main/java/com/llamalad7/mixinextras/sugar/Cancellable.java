package com.llamalad7.mixinextras.sugar;

import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Allows you to receive a cancellable {@link CallbackInfo} or {@link CallbackInfoReturnable} as appropriate
 * from any kind of injector. This allows you to optionally cancel the target method without being forced to use
 * {@link Inject @Inject}.
 * <p>
 * The same {@link CallbackInfo}s will be passed to every handler method in a chain of
 * {@link WrapOperation @WrapOperation}s (i.e. any number of {@link WrapOperation @WrapOperation}s and at most one inner
 * {@link Redirect @Redirect} / {@link ModifyConstant @ModifyConstant}). This means you can choose to use the
 * {@link CallbackInfo#isCancelled()} and {@link CallbackInfoReturnable#getReturnValue()} methods to see if the wrapped
 * handler cancelled, so you can respond accordingly.
 * <p>
 * See <a href="https://github.com/LlamaLad7/MixinExtras/wiki/Cancellable">the wiki article</a> for more info.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.CLASS)
public @interface Cancellable {
}
