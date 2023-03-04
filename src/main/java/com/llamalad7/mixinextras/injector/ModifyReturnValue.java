package com.llamalad7.mixinextras.injector;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>NB: Not to be confused with {@link ModifyExpressionValue}</p>
 *
 * <p>Allows you to tweak the value being returned from a method.</p>
 *
 * <p>Your handler method receives the value about to be returned (optionally
 * followed by the enclosing method's parameters),
 * and should return the adjusted value.</p>
 *
 * <p>Should be used in favour of {@link org.spongepowered.asm.mixin.injection.Inject Inject} with the
 * <code>cir.setReturnValue(modify(cir.getReturnValue())</code> pattern, as that pattern
 * does not chain when multiple people do it, whereas this does.</p>
 *
 * <br><h2>Example</h2>
 *
 * <p>When targeting code such as the following: <br><br>
 * <code>return this.speed * 10f - 0.5f;</code> <br><br>
 * you may wish to change the value being returned, e.g. by dividing it by 2.<br>
 *
 * This could be done like so: <br><br>
 *
 * <pre>
 * &#064;ModifyReturnValue(
 * 	method = "targetMethod",
 * 	at = @At("RETURN")
 * )
 * private float halveSpeed(float original) {
 * 	return original / 2f;
 * }
 * </pre>
 * Your handler method would then be called with the result of the
 * existing calculation, and you could change the value as it is returned.<br><br>
 *
 * Multiple mods can do this at the same time, aand all their conditions will be applied.
 * </p><br><br>
 *
 * <h3>Code Diff</h3>
 * <pre>
 * <span style="background-color:#67060c"> - return this.speed * 10f - 0.5f;</span>
 * <span style="background-color:#033a16"> + return halveSpeed(this.speed * 10f - 0.5f);</span>
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ModifyReturnValue {
    String[] method();

    At[] at();

    Slice[] slice() default {};

    boolean remap() default true;

    int require() default -1;

    int expect() default 1;

    int allow() default -1;
}
