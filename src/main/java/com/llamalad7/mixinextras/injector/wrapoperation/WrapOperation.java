package com.llamalad7.mixinextras.injector.wrapoperation;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Slice;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Allows you to wrap a method call, field get/set, or <code>instanceof</code> check.</p>
 *
 * <p>Your handler method receives the targeted instruction's arguments and
 * an {@link Operation} representing the operation being wrapped (optionally
 * followed by the enclosing method's parameters). You should return the same
 * type as the wrapped operation does.</p>
 *
 * <p>Should be used in favour of {@link org.spongepowered.asm.mixin.injection.Redirect Redirect} when you are wrapping the original
 * operation and not replacing it outright, as unlike {@link org.spongepowered.asm.mixin.injection.Redirect Redirect}, this chains when used by multiple people.</p>
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
 *     <td> Non-static method call </td> <td> private ReturnType yourHandlerMethod(OwnerType type, &lt;args of the original call&gt;, Operation&lt;ReturnType&gt; original) </td>
 *   </tr>
 *   <tr>
 *     <td> Static method call </td> <td> private ReturnType yourHandlerMethod(&lt;args of the original call&gt;, Operation&lt;ReturnType&gt; original) </td>
 *   </tr>
 *   <tr>
 *     <td> Non-static field get </td> <td> private FieldType yourHandlerMethod(OwnerType type, Operation&lt;FieldType&gt; original) </td>
 *   </tr>
 *   <tr>
 *     <td> Static field get </td> <td> private FieldType yourHandlerMethod(Operation&lt;FieldType&gt; original) </td>
 *   </tr>
 *   <tr>
 *     <td> Non-static field write </td> <td> private void yourHandlerMethod(OwnerType type, FieldType newValue, Operation&lt;Void&gt; original) </td>
 *   </tr>
 *   <tr>
 *     <td> Static field write </td> <td> private void yourHandlerMethod(FieldType newValue, Operation&lt;Void&gt; original) </td>
 *   </tr>
 *   <tr>
 *     <td> <code>instanceof</code> check </td> <td> private boolean yourHandlerMethod(Object obj, Operation&lt;Boolean&gt; original) </td>
 *   </tr>
 * </table><br>
 * <p>In all of these cases, you can optionally add the enclosing method's parameters to the end,
 * should you require them for additional context.</p>
 *
 * <br><h2>Example</h2>
 *
 * <p>When targeting code such as the following: <br><br>
 * <code>int number = this.expensiveCalculation(flag);</code> <br><br>
 * you may wish to wrap the call such that it is bypassed if a setting is enabled.<br>
 *
 * This could be done like so: <br><br>
 *
 * <pre>
 * &#064;WrapOperation(
 *     method = "targetMethod",
 *     at = @At(value = "INVOKE", target = "Lsome/package/TargetClass;expensiveCalculation(Z)I")
 * )
 * private int skipExpensiveMethodIfNeeded(TargetClass target, boolean flag, Operation&lt;Integer&gt; original) {
 *     if (YourMod.bypassExpensiveCalculation) {
 *         return 10;
 *     } else {
 *         return original.call(instance, flag);
 *     }
 * }
 * </pre>
 * <code>expensiveCalculation</code> would then only be called if you called it yourself via the <code>original.call(...)</code> invocation.<br><br>
 *
 * Multiple mods can do this at the same time, and the wrapping will chain.
 * The first to be applied receives an {@link Operation} representing the vanilla call,
 * if another is applied it receives an {@link Operation} representing the first one's wrapping, etc.
 * </p><br><br>
 *
 * <h3>Code Diff</h3>
 * <pre>
 * <span style="background-color:#67060c"> - int number = this.expensiveCalculation(flag);</span>
 * <span style="background-color:#033a16"> + int number = this.bypassExpensiveCalculationIfNecessary(this, flag, args -> {</span>
 * <span style="background-color:#033a16"> +         return ((TargetClass) args[0]).expensiveCalculation((Boolean) args[1]);</span>
 * <span style="background-color:#033a16"> + });</span>
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface WrapOperation {
    String[] method();

    /**
     * Selector for targeting method calls and field gets/sets.
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
}
