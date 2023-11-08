package com.llamalad7.mixinextras.utils.info;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.Expression;
import com.llamalad7.mixinextras.expression.impl.ast.identifiers.Identifier;
import com.llamalad7.mixinextras.expression.impl.serialization.ExpressionAdapter;
import com.llamalad7.mixinextras.expression.impl.serialization.IdentifierAdapter;

public class ExtraMixinInfoSerializer {
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Expression.class, new ExpressionAdapter())
            .registerTypeAdapter(Identifier.class, new IdentifierAdapter())
            .disableHtmlEscaping()
            .create();

    public static String serialize(ExtraMixinInfo info) {
        return GSON.toJson(info);
    }

    public static ExtraMixinInfo deSerialize(String str) {
        return GSON.fromJson(str, ExtraMixinInfo.class);
    }
}
