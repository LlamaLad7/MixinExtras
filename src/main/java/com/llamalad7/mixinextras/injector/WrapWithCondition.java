package com.llamalad7.mixinextras.injector;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Allows you to wrap a field write or void method call with a conditional check.</p>
 *
 * <p>Your handler method receives the targeted instruction's arguments
 * (optionally followed by the enclosing method's parameters), and should
 * return a boolean indicating whether the operation should go ahead.</p>
 *
 * <p>Should be used in favour of {@link org.spongepowered.asm.mixin.injection.Redirect Redirect} when you are simply
 * wrapping the original call with a check, as unlike {@link org.spongepowered.asm.mixin.injection.Redirect Redirect},
 * this chains when used by multiple people.</p>
 *
 * <br><h2>Handler Methods</h2>
 * <table border="1">
 *   <thead>
 *     <tr>
 *       <th>Targeted operation</th>
 *       <th>Handler signature</th>
 *     </tr>
 *   </thead>
 *   <tr>
 *     <td> Non-static method call </td> <td> private boolean yourHandlerMethod(OwnerType type, &lt;args of the original call&gt;) </td>
 *   </tr>
 *   <tr>
 *     <td> Static method call </td> <td> private boolean yourHandlerMethod(&lt;args of the original call&gt;) </td>
 *   </tr>
 *   <tr>
 *     <td> Non-static field write </td> <td> private boolean yourHandlerMethod(OwnerType type, FieldType newValue) </td>
 *   </tr>
 *   <tr>
 *     <td> Static field write </td> <td> private boolean yourHandlerMethod(FieldType newValue) </td>
 *   </tr>
 * </table><br>
 * <p>In all of these cases, you can optionally add the enclosing method's parameters to the end,
 * should you require them for additional context.</p>
 *
 * <br><h2>Example</h2>
 *
 * <p>When targeting code such as the following: <br><br>
 * <code>this.render(this.tickDelta);</code> <br><br>
 * you may wish to wrap the call in a conditional check, e.g. <code>if (YourMod.shouldRender())</code><br>
 *
 * This could be done like so: <br><br>
 *
 * <pre>
 * &#064;WrapWithCondition(
 * 	method = "targetMethod",
 * 	at = @At(value = "INVOKE", target = "Lsome/package/TargetClass;render(F)V")
 * )
 * private boolean onlyRenderIfAllowed(TargetClass instance, float tickDelta) {
 * 	return YourMod.shouldRender();
 * }
 * </pre>
 * <code>render</code> would then only be called if your handler method returns <code>true</code>.<br><br>
 *
 * Multiple mods can do this at the same time, aand all their conditions will be applied.
 * </p><br><br>
 *
 * <h3>Code Diff</h3>
 * <pre>
 * <span style="background-color:#033a16"> + if (onlyRenderIfAllowed(this, this.tickDelta)) {</span>
 * <span>     this.render(this.tickDelta);</span>
 * <span style="background-color:#033a16"> + }</span>
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface WrapWithCondition {
    String[] method();

    At[] at();

    Slice[] slice() default {};

    boolean remap() default true;

    int require() default -1;

    int expect() default 1;

    int allow() default -1;
}
