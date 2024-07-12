package com.llamalad7.mixinextras.expression.impl.utils;

import com.llamalad7.mixinextras.expression.impl.flow.postprocessing.LMFInfo;

public class FlowDecorations {
    /**
     * "Persistent" decorations will be copied to target nodes from flow nodes.
     */
    public static final String PERSISTENT = "mixinextras_persistent_";

    /**
     * Stores information about this array creation.
     */
    public static final String ARRAY_CREATION_INFO = PERSISTENT + "arrayCreationInfo";

    /**
     * Stores information about this part of a string concatenation.
     */
    public static final String STRING_CONCAT_INFO = "stringConcatInfo";

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

    /**
     * Stores the {@link LMFInfo} of this LMF invocation.
     */
    public static final String LMF_INFO = "lmfInfo";
}
