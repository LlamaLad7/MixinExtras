package com.llamalad7.mixinextras.injector;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Allows you to tweak the resultant value of a method call,
 * field get, constant, or new object instantiation.</p>
 *
 * <p>Your handler method receives the expression's resultant value
 * (optionally followed by the enclosing method's parameters),
 * and should return the adjusted value.</p>
 *
 * <p>Should be used in favour of {@link org.spongepowered.asm.mixin.injection.Redirect Redirect} or
 * {@link org.spongepowered.asm.mixin.injection.ModifyConstant ModifyConstant} when you have to
 * tweak an expression's existing value instead of replacing it entirely, as unlike
 * {@link org.spongepowered.asm.mixin.injection.Redirect Redirect} and
 * {@link org.spongepowered.asm.mixin.injection.ModifyConstant ModifyConstant}, this
 * chains when used by multiple people</p>
 *
 * <br><h2>Example</h2>
 *
 * <p>When targeting code such as the following: <br><br>
 * <pre>
 * if (this.shouldFly()) {
 * 	this.fly();
 * }
 * </pre>
 * you may wish to add an extra condition, e.g. <code>&& YourMod.isFlyingAllowed()</code><br>
 *
 * This could be done like so: <br><br>
 *
 * <pre>
 * &#064;ModifyExpressionValue(
 * 	method = "targetMethod",
 * 	at = @At(value = "INVOKE", target = "Ltarget/Class;shouldFly()Z")
 * )
 * private boolean onlyFlyIfAllowed(boolean original) {
 * 	return original && YourMod.isFlyingAllowed();
 * }
 * </pre>
 * Your handler method would then be called with the result of <code>shouldFly</code>,
 * and you could prevent flying should you wish.<br><br>
 *
 * Multiple mods can do this at the same time, and all their modifications will be applied.
 * </p><br><br>
 *
 * <h3>Code Diff</h3>
 * <pre>
 * <span style="background-color:#67060c"> - if (this.shouldFly()) {</span>
 * <span style="background-color:#033a16"> + if (onlyFlyIfAllowed(this.shouldFly())) {</span>
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ModifyExpressionValue {
    String[] method();

    At[] at();

    Slice[] slice() default {};

    boolean remap() default true;

    int require() default -1;

    int expect() default 1;

    int allow() default -1;
}
