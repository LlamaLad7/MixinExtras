package com.llamalad7.mixinextras.sugar.impl;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

public class SugarParameter {
    public final AnnotationNode sugar;
    public final Type type;
    public final Type genericType;
    public final int lvtIndex;
    public final int paramIndex;

    SugarParameter(AnnotationNode sugar, Type type, Type genericType, int lvtIndex, int paramIndex) {
        this.sugar = sugar;
        this.type = type;
        this.genericType = genericType;
        this.lvtIndex = lvtIndex;
        this.paramIndex = paramIndex;
    }
}
