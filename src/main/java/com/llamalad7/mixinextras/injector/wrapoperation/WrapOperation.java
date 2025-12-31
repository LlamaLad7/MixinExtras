package com.llamalad7.mixinextras.injector.wrapoperation;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Slice;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Allows you to wrap many kinds of operations.
 * <p>
 * It accepts these injection points:
 * {@link org.spongepowered.asm.mixin.injection.points.BeforeInvoke INVOKE},
 * {@link org.spongepowered.asm.mixin.injection.points.BeforeFieldAccess FIELD},
 * {@link org.spongepowered.asm.mixin.injection.Constant @Constant},
 * {@link org.spongepowered.asm.mixin.injection.points.BeforeNew NEW} and
 * {@link com.llamalad7.mixinextras.expression.Expression MIXINEXTRAS:EXPRESSION}.
 * <p>
 * Your handler method receives the targeted instruction's arguments and an {@link Operation} representing the operation
 * being wrapped (optionally followed by the enclosing method's parameters).
 * You should return the same type as the wrapped operation does:
 * <table width="100%">
 *   <tr>
 *     <th width="25%">Targeted operation</th>
 *     <th>Handler signature</th>
 *   </tr>
 *   <tr>
 *     <td>Non-static method call</td>
 *     <td><code>private (static) <b>ReturnType</b> handler(<b>ReceiverType</b> instance, <b>&lt;params of the original
 *     call&gt;</b>, Operation&lt;<b>ReturnType</b>&gt; original)</code></td>
 *   </tr>
 *   <tr>
 *     <td><code>super.</code> method call</td>
 *     <td><code>private (static) <b>ReturnType</b> handler(<b>ThisClass</b> instance, <b>&lt;params of the original
 *     call&gt;</b>, Operation&lt;<b>ReturnType</b>&gt; original)</code></td>
 *   </tr>
 *   <tr>
 *     <td>Static method call</td>
 *     <td><code>private (static) <b>ReturnType</b> handler(<b>&lt;params of the original call&gt;</b>,
 *     Operation&lt;<b>ReturnType</b>&gt; original)</code></td>
 *   </tr>
 *   <tr>
 *     <td>Non-static field get</td>
 *     <td><code>private (static) <b>FieldType</b> handler(<b>ReceiverType</b> instance,
 *     Operation&lt;<b>FieldType</b>&gt; original)</code></td>
 *   </tr>
 *   <tr>
 *     <td>Static field get</td>
 *     <td><code>private (static) <b>FieldType</b> handler(Operation&lt;<b>FieldType</b>&gt; original)</code></td>
 *   </tr>
 *   <tr>
 *     <td>Non-static field write</td>
 *     <td><code>private (static) void handler(<b>ReceiverType</b> instance, <b>FieldType</b> newValue,
 *     Operation&lt;Void&gt; original)</code></td>
 *   </tr>
 *   <tr>
 *     <td>Static field write</td>
 *     <td><code>private (static) void handler(<b>FieldType</b> newValue, Operation&lt;Void&gt; original)</code></td>
 *   </tr>
 *   <tr>
 *     <td><code>instanceof</code> check</td>
 *     <td><code>private (static) boolean handler(Object obj, Operation&lt;Boolean&gt; original)</code></td>
 *   </tr>
 *   <tr>
 *     <td>Object instantiation</td>
 *     <td><code>private (static) <b>ObjectType</b> handler(<b>&lt;params of the original ctor&gt;</b>,
 *     Operation&lt;<b>ObjectType</b>&gt; original)</code></td>
 *   </tr>
 *   <tr>
 *     <td>Primitive comparison</td>
 *     <td><code>private (static) boolean handler(<b>theType</b> left, <b>theType</b> right,
 *     Operation&lt;Boolean&gt; original)</code></td>
 *   </tr>
 *   <tr>
 *     <td>Reference comparison</td>
 *     <td><code>private (static) boolean handler(Object left, Object right,
 *     Operation&lt;Boolean&gt; original)</code></td>
 *   </tr>
 *   <tr>
 *     <td>Array element get</td>
 *     <td><code>private (static) <b>ElementType</b> handler(<b>ElementType</b>[] array, int index,
 *     Operation&lt;<b>ElementType</b>&gt; original)</code></td>
 *   </tr>
 *   <tr>
 *     <td>Array element set</td>
 *     <td><code>private (static) void handler(<b>ElementType</b>[] array, int index, <b>ElementType</b> value,
 *     Operation&lt;Void&gt; original)</code></td>
 *   </tr>
 *   <tr>
 *     <td>Object cast</td>
 *     <td><code>private (static) <b>CastType</b> handler(Object object,
 *     Operation&lt;<b>CastType</b>&gt; original)</code></td>
 *   </tr>
 * </table>
 * When {@code call}ing the {@code original}, you must pass everything before the {@code original} in your handler's
 * parameters. You can optionally pass different values to change what the {@code original} uses.
 * <p>
 * This chains when used by multiple people, unlike
 * {@link org.spongepowered.asm.mixin.injection.Redirect @Redirect} and
 * {@link org.spongepowered.asm.mixin.injection.ModifyConstant @ModifyConstant}.
 * <p>
 * <b>If you never call the {@code original} then you risk other people's code being silently ignored.</b>
 * <p>
 * See <a href="https://github.com/LlamaLad7/MixinExtras/wiki/WrapOperation">the wiki article</a> for more info.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface WrapOperation {
    String[] method();

    /**
     * Selector for targeting method calls, field gets/sets and object instantiations.
     */
    At[] at() default {};

    /**
     * Selector for targeting `instanceof`s.
     */
    Constant[] constant() default {};

    Slice[] slice() default {};

    boolean remap() default true;

    int require() default -1;

    int expect() default 1;

    int allow() default -1;

    /**
     * Application order relative to other @WrapOperation injectors.
     * Only respected on Mixin 0.8.7+
     */
    int order() default 1000;
}
