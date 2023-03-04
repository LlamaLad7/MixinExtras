package com.llamalad7.mixinextras.sugar;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Sugar allows you take extra parameters in any kind of injector,
 * whether it be an {@link org.spongepowered.asm.mixin.injection.Inject @Inject},
 * a {@link org.spongepowered.asm.mixin.injection.Redirect @Redirect},
 * a {@link org.spongepowered.asm.mixin.injection.ModifyArg @ModifyArg},
 * etc, as well as all of MixinExtras' custom ones.</p>
 *
 * <p>Sugared parameters must always be at the end of your handler's
 * parameters, but you can have as many as you like in any order.</p>
 *
 * <p>Currently there is only one type of sugar available, {@link Local @Local},
 * but more will be available in future.</p>
 *
 * <br><h2>{@link Local @Local}</h2>
 *
 * <p>Allows you to capture local variables wherever you need them.
 * Targeting the variables works in exactly the same way as explained
 * {@link org.spongepowered.asm.mixin.injection.ModifyVariable here},
 * so I encourage you to read that.</p>
 *
 * <p>Possible parameters could look like:<br>
 * <ul>
 *     <li><code>@Local double fov</code> (captures the only local of type <code>double</code> and throws an error if there is more than one)</li>
 *     <li><code>@Local(ordinal = 1) BlockPos pos</code> (captures the second local of type <code>BlockPos</code>)</li>
 *     <li><code>@Local(index = 3) int x</code> (captures the local with LVT index 3)</li>
 * </ul>
 * </p>
 *
 * <br><h2>Example</h2>
 *
 * <p>When targeting code such as the following: <br><br>
 * <pre>
 * boolean bl1 = complexThing1() && complexThing2() || complexThing3();
 * boolean bl2 = !complexThing4() || complexThing5();
 * </pre>
 * you may wish to change <code>bl2</code> in such a way that requires the value of <code>bl1</code>.<br>
 *
 * This could be done like so: <br><br>
 *
 * <pre>
 * &#064;ModifyVariable(method = "targetMethod", at = @At("STORE"), ordinal = 1)
 * private boolean modifybl2ForReasons(boolean original, @Local(ordinal = 0) boolean bl1) {
 * 	return original && YourMod.anotherComplexThing(bl1);
 * }
 * </pre>
 * Your <code>ModifyVariable</code> would work as normal, modifying <code>bl2</code>,
 * but you also receive the value of <code>bl1</code>.<br><br>
 *
 * Multiple mods can do this at the same time, and all their modifications will be applied.
 * </p><br><br>
 *
 * <h3>Code Diff</h3>
 * <pre>
 * <span>   boolean bl2 = !complexThing4() || complexThing5();</span>
 * <span style="background-color:#033a16"> + bl2 = modifybl2ForReasons(bl2, bl1);</span>
 * </pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.CLASS)
public @interface Local {
    boolean print() default false;

    int ordinal() default -1;

    int index() default -1;

    String[] name() default {};

    boolean argsOnly() default false;
}
