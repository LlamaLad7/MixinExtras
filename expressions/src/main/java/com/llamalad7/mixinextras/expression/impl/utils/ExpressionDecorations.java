package com.llamalad7.mixinextras.expression.impl.utils;

public class ExpressionDecorations {
    /**
     * Stores the type of a simple expression.
     */
    public static final String SIMPLE_EXPRESSION_TYPE = "mixinextras_simpleExpressionType";

    /**
     * Stores injector-specific information about modified comparisons.
     */
    public static final String COMPARISON_INFO = "mixinextras_comparisonInfo";

    /**
     * Stores the arguments of this operation if they have been offered by an Expression.
     */
    public static final String SIMPLE_OPERATION_ARGS = "mixinextras_simpleOperationArgs";

    /**
     * Stores suggested parameter names for the handler of this operation, for use by MCDev.
     */
    public static final String SIMPLE_OPERATION_PARAM_NAMES = "mixinextras_simpleOperationParamNames";

    /**
     * Stores the return type of this operation if it has been offered by an Expression.
     */
    public static final String SIMPLE_OPERATION_RETURN_TYPE = "mixinextras_simpleOperationReturnType";

    /**
     * Stores information about this compound instruction.
     */
    public static final String EXPANSION_INFO = "mixinextras_expansionInfo";

    /**
     * Stores that this StringBuilder#append call was targeted as part of a String concatenation expression, and should
     * therefore be modifiable as though it were a String.
     */
    public static final String IS_STRING_CONCAT_EXPRESSION = "mixinextras_isStringConcatExpression";
}
