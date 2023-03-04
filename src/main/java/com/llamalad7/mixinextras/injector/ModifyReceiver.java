package com.llamalad7.mixinextras.injector;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Allows you to modify the receiver of a non-static method call or field get/set.</p>
 *
 * <p>Your handler method receives the targeted instruction's arguments
 * (optionally followed by the enclosing method's parameters),
 * and should return the adjusted receiver for the operation.</p>
 *
 * <p>Should be used in favour of {@link org.spongepowered.asm.mixin.injection.Redirect Redirect}
 * when you are simply inspecting or conditionally modifying the receiver of an operation,
 * as unlike {@link org.spongepowered.asm.mixin.injection.Redirect Redirect},
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
 *     <td> Non-static method call </td> <td> private OwnerType yourHandlerMethod(OwnerType receiver, &lt;args of the original call&gt;) </td>
 *   </tr>
 *   <tr>
 *     <td> Non-static field write </td> <td> private OwnerType yourHandlerMethod(OwnerType receiver, FieldType newValue) </td>
 *   </tr>
 *   <tr>
 *     <td> Non-static field read </td> <td> private OwnerType yourHandlerMethod(OwnerType receiver) </td>
 *   </tr>
 * </table><br>
 * <p>In all of these cases, you can optionally add the enclosing method's parameters to the end,
 * should you require them for additional context.</p>
 *
 * <br><h2>Example</h2>
 *
 * <p>When targeting code such as the following: <br><br>
 * <code>object1.setX(newXPosition);</code> <br><br>
 * you may wish to inspect or change the object being moved.<br>
 *
 * This could be done like so: <br><br>
 *
 * <pre>
 * &#064;ModifyReceiver(
 * 	method = "targetMethod",
 * 	at = @At(value = "INVOKE", target = "Lsome/package/TargetClass;setX(I)V")
 * )
 * private TargetClass changeObject(TargetClass receiver, int newX) {
 *         if (newX == 10) {
 * 	        return object2;
 *         }
 *         return receiver;
 * }
 * </pre>
 * <code>setX/code> would then be called on <code>object2</code> instead, if you so desired.<br><br>
 *
 * Multiple mods can do this at the same time, and all their modifications will be applied.
 * </p><br><br>
 *
 * <h3>Code Diff</h3>
 * <pre>
 * <span style="background-color:#67060c"> - object1.setX(10);</span>
 * <span style="background-color:#033a16"> + changeObject(object1, 10).setX(10);</span>
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ModifyReceiver {
    String[] method();

    At[] at();

    Slice[] slice() default {};

    boolean remap() default true;

    int require() default -1;

    int expect() default 1;

    int allow() default -1;
}
