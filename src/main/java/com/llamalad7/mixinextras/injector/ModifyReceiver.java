package com.llamalad7.mixinextras.injector;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Allows you to modify the receiver of a non-static
 * {@link org.spongepowered.asm.mixin.injection.points.BeforeInvoke method call} or
 * {@link org.spongepowered.asm.mixin.injection.points.BeforeFieldAccess field get/set}.
 * <p>
 * Your handler method receives the targeted instruction's arguments (optionally followed by the enclosing method's
 * parameters), and should return the adjusted receiver for the operation.
 * <table width="100%">
 *   <tr>
 *     <th width="25%">Targeted operation</th>
 *     <th>Handler signature</th>
 *   </tr>
 *   <tr>
 *     <td>Non-static method call</td>
 *     <td><code>private (static) <b>ReceiverType</b> handler(<b>ReceiverType</b> original, <b>&lt;params of the
 *     original call&gt;</b>)</code></td>
 *   </tr>
 *   <tr>
 *     <td><code>super.</code> method call</td>
 *     <td><code>private (static) <b>ThisClass</b> handler(<b>ThisClass</b> original, <b>&lt;params of the original
 *     call&gt;</b>)</code></td>
 *   </tr>
 *   <tr>
 *     <td>Non-static field get</td>
 *     <td><code>private (static) <b>ReceiverType</b> handler(<b>ReceiverType</b> original)</code></td>
 *   </tr>
 *   <tr>
 *     <td>Non-static field write</td>
 *     <td><code>private (static) <b>ReceiverType</b> handler(<b>ReceiverType</b> original, <b>FieldType</b> newValue)</code>
 *     </td>
 *   </tr>
 * </table>
 * This chains when used by multiple people, unlike
 * {@link org.spongepowered.asm.mixin.injection.Redirect @Redirect}.
 * <p>
 * <b>If you never use the {@code original} then you risk other people's changes being silently ignored.</b>
 * <p>
 * See <a href="https://github.com/LlamaLad7/MixinExtras/wiki/ModifyReceiver">the wiki article</a> for more info.
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
