package com.llamalad7.mixinextras.expression.impl.serialization;

import com.google.gson.*;
import com.llamalad7.mixinextras.expression.impl.ast.identifiers.Identifier;
import com.llamalad7.mixinextras.expression.impl.ast.identifiers.PoolIdentifier;
import com.llamalad7.mixinextras.expression.impl.ast.identifiers.WildcardIdentifier;

import java.lang.reflect.Type;

public class IdentifierAdapter implements JsonSerializer<Identifier>, JsonDeserializer<Identifier> {
    @Override
    public JsonElement serialize(Identifier src, Type typeOfSrc, JsonSerializationContext context) {
        if (src instanceof PoolIdentifier) {
            return new JsonPrimitive(((PoolIdentifier) src).name);
        }
        return new JsonPrimitive('?');
    }

    @Override
    public Identifier deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        String str = json.getAsString();
        if (str.equals("?")) {
            return new WildcardIdentifier();
        }
        return new PoolIdentifier(str);
    }
}
