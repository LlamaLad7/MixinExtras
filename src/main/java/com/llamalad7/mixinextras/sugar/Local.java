package com.llamalad7.mixinextras.sugar;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Allows you to capture local variables wherever you need them. The annotated parameter's type must either match the
 * target variable's type, or be the corresponding
 * {@link com.llamalad7.mixinextras.sugar.ref.LocalRef LocalRef}
 * type. In the latter case you can both read from and write to the target variable.
 * <p>
 * Targeting the variables can be done in 2 ways:
 * <ul>
 *     <li><b>Explicit Mode</b>: The variable to target is determined by {@code ordinal}, {@code index} or {@code name}.</li>
 *     <li><b>Implicit Mode</b>: You don't specify any of the above. If there is exactly one variable of the targeted type
 *     available, that will be targeted. If not, an error is thrown.</li>
 * </ul>
 * See <a href="https://github.com/LlamaLad7/MixinExtras/wiki/Local">the wiki article</a> for more info.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.CLASS)
public @interface Local {
    /**
     * Whether to print a table of the available variables so you can determine the right discriminators to use.
     * <b>This will abort the injection.</b>
     */
    boolean print() default false;

    /**
     * The index of the local variable by type. E.g. if there are 3 {@code String} variables, an ordinal of 2 would
     * target the 3rd one.
     */
    int ordinal() default -1;

    /**
     * The LVT index of the local variable. Get this from the bytecode or using {@link Local#print}. This is generally
     * more brittle than {@code ordinal}.
     */
    int index() default -1;

    /**
     * Names of the local variable. <b>This will not work on obfuscated code like Minecraft</b>. For targeting
     * unobfuscated code this is normally the least brittle option.
     */
    String[] name() default {};

    /**
     * Whether only the method's parameters should be included. This makes the capture more efficient.
     */
    boolean argsOnly() default false;
}
