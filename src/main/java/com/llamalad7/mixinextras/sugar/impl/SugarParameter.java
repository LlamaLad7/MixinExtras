package com.llamalad7.mixinextras.sugar.impl;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

public class SugarParameter {
    public final AnnotationNode sugar;
    public final Type type;
    public final int lvtIndex;

    SugarParameter(AnnotationNode sugar, Type type, int lvtIndex) {
        this.sugar = sugar;
        this.type = type;
        this.lvtIndex = lvtIndex;
    }
}
