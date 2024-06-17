package com.llamalad7.mixinextras.utils;

import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;

public class Decorations {
    /**
     * "Persistent" decorations will be copied to handler method calls which targeted the original instruction.
     * This should be used as a prefix to an existing decoration key.
     */
    public static final String PERSISTENT = "mixinextras_persistent_";
    /**
     * Stores that a non-void operation has its result immediately popped and so can be treated as void by someone
     * using {@link WrapWithCondition} on it.
     */
    public static final String POPPED_OPERATION = "mixinextras_operationIsImmediatelyPopped";
    /**
     * Stores a map of LVT index of target local -&gt; LVT index of applicable {@link LocalRef}.
     * References should be shared by injectors that wrap/replace the same target instruction.
     */
    public static final String LOCAL_REF_MAP = "mixinextras_localRefMap";
    /**
     * Stores that a NEW instruction is immediately DUPed so that {@link WrapOperation} can handle it properly.
     */
    public static final String NEW_IS_DUPED = "mixinextras_newIsDuped";
    /**
     * Stores the types that a NEW's &lt;init&gt; takes as its parameters.
     */
    public static final String NEW_ARG_TYPES = "mixinextras_newArgTypes";

    /**
     * Stores that this node has been wrapped by a {@link WrapOperation}.
     */
    public static final String WRAPPED = "mixinextras_wrappedOperation";

    /**
     * Stores the shared CallbackInfo local index for this target instruction.
     */
    public static final String CANCELLABLE_CI_INDEX = "mixinextras_cancellableCiIndex";

    /**
     * Stores the type of a simple expression targeted by an {@link Expression}.
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
     * Stores information about this array creation.
     */
    public static final String ARRAY_CREATION_INFO = PERSISTENT + "arrayCreationInfo";

    /**
     * Stores information about this compound instruction.
     */
    public static final String EXPANSION_INFO = "mixinextras_expansionInfo";

    /**
     * Stores information about this part of a string concatenation.
     */
    public static final String STRING_CONCAT_INFO = "stringConcatInfo";

    /**
     * Stores that this StringBuilder#append call was targeted as part of a String concatenation expression, and should
     * therefore be modifiable as though it were a String.
     */
    public static final String IS_STRING_CONCAT_EXPRESSION = "mixinextras_isStringConcatExpression";

    /**
     * Marks that this @At is contained within a @Slice.
     */
    public static final String IS_IN_SLICE = "mixinextras_isInSlice";

    /**
     * Stores information about this object instantiation.
     */
    public static final String INSTANTIATION_INFO = "instantiationInfo";

    /**
     * Stores the jump instruction for this complex comparison.
     */
    public static final String COMPLEX_COMPARISON_JUMP = "complexComparisonJump";

    /**
     * Stores the type of this method call.
     */
    public static final String METHOD_CALL_TYPE = "methodCallType";
}
