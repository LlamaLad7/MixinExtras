package com.llamalad7.mixinextras.expression.impl.serialization;

import com.google.gson.*;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.*;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExpressionAdapter implements JsonSerializer<Expression>, JsonDeserializer<Expression> {
    private static final List<Class<? extends Expression>> EXPRESSION_TYPES = Arrays.asList(
            ArrayAccessExpression.class,
            ArrayStoreExpression.class,
            BinaryExpression.class,
            BooleanLiteralExpression.class,
            CapturingExpression.class,
            CastExpression.class,
            ComparisonExpression.class,
            DecimalLiteralExpression.class,
            IdentifierAssignmentExpression.class,
            IdentifierExpression.class,
            InstanceofExpression.class,
            InstantiationExpression.class,
            IntLiteralExpression.class,
            MemberAccessExpression.class,
            MemberAssignmentExpression.class,
            MethodCallExpression.class,
            NullLiteralExpression.class,
            StaticMethodCallExpression.class,
            StringLiteralExpression.class,
            UnaryExpression.class,
            WildcardExpression.class
    );
    private static final Map<String, Class<? extends Expression>> INDEX = new HashMap<>();

    static {
        for (Class<? extends Expression> clazz : EXPRESSION_TYPES) {
            INDEX.put(clazz.getAnnotation(SerializedTypeName.class).value(), clazz);
        }
    }

    @Override
    public JsonElement serialize(Expression src, Type typeOfSrc, JsonSerializationContext context) {
        JsonElement result = context.serialize(src);
        String typeCode = src.getClass().getAnnotation(SerializedTypeName.class).value();
        result.getAsJsonObject().addProperty("_t", typeCode);
        return result;
    }

    @Override
    public Expression deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        Class<? extends Expression> type = INDEX.get(json.getAsJsonObject().get("_t").getAsString());
        return context.deserialize(json, type);
    }
}
