package com.llamalad7.mixinextras.expression.impl.serialization;

import com.llamalad7.mixinextras.expression.impl.ast.expressions.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExpressionReading {
    private static final Map<String, Reader> ID_TO_READER = new HashMap<>();

    static {
        List<Mapping> mappings = Arrays.asList(
                new Mapping(ArrayAccessExpression.class, ArrayAccessExpression::read),
                new Mapping(ArrayStoreExpression.class, ArrayStoreExpression::read),
                new Mapping(BinaryExpression.class, BinaryExpression::read),
                new Mapping(BooleanLiteralExpression.class, BooleanLiteralExpression::read),
                new Mapping(CapturingExpression.class, CapturingExpression::read),
                new Mapping(CastExpression.class, CastExpression::read),
                new Mapping(ComparisonExpression.class, ComparisonExpression::read),
                new Mapping(DecimalLiteralExpression.class, DecimalLiteralExpression::read),
                new Mapping(IdentifierAssignmentExpression.class, IdentifierAssignmentExpression::read),
                new Mapping(IdentifierExpression.class, IdentifierExpression::read),
                new Mapping(InstanceofExpression.class, InstanceofExpression::read),
                new Mapping(InstantiationExpression.class, InstantiationExpression::read),
                new Mapping(IntLiteralExpression.class, IntLiteralExpression::read),
                new Mapping(MemberAccessExpression.class, MemberAccessExpression::read),
                new Mapping(MemberAssignmentExpression.class, MemberAssignmentExpression::read),
                new Mapping(MethodCallExpression.class, MethodCallExpression::read),
                new Mapping(NullLiteralExpression.class, NullLiteralExpression::read),
                new Mapping(ReturnExpression.class, ReturnExpression::read),
                new Mapping(StaticMethodCallExpression.class, StaticMethodCallExpression::read),
                new Mapping(StringLiteralExpression.class, StringLiteralExpression::read),
                new Mapping(ThrowExpression.class, ThrowExpression::read),
                new Mapping(UnaryExpression.class, UnaryExpression::read),
                new Mapping(WildcardExpression.class, WildcardExpression::read)
        );
        for (Mapping mapping : mappings) {
            ID_TO_READER.put(mapping.id, mapping.reader);
        }
    }

    public static Expression read(ExpressionReader reader) throws IOException {
        return ID_TO_READER.get(reader.readString()).read(reader);
    }

    private static class Mapping {
        private final String id;
        private final Reader reader;

        public Mapping(Class<? extends Expression> clazz, Reader reader) {
            this.id = clazz.getAnnotation(SerializedExpressionId.class).value();
            this.reader = reader;
        }
    }

    @FunctionalInterface
    private interface Reader {
        Expression read(ExpressionReader reader) throws IOException;
    }
}
