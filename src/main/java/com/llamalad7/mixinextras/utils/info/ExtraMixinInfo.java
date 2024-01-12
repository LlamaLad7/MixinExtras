package com.llamalad7.mixinextras.utils.info;

import com.llamalad7.mixinextras.expression.impl.ast.expressions.Expression;
import com.llamalad7.mixinextras.expression.impl.serialization.ExpressionReader;
import com.llamalad7.mixinextras.expression.impl.serialization.ExpressionWriter;
import com.llamalad7.mixinextras.service.MixinExtrasVersion;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ExtraMixinInfo {
    private final int version;
    private final Map<String, Expression> parsedExpressions;

    public ExtraMixinInfo() {
        version = MixinExtrasVersion.LATEST.getNumber();
        parsedExpressions = new HashMap<>();
    }

    public ExtraMixinInfo(InputStream file) throws IOException {
        try (DataInputStream input = new DataInputStream(file)) {
            version = input.readInt();
            int expressionCount = input.readInt();
            ExpressionReader reader = new ExpressionReader(version, input);
            parsedExpressions = new HashMap<>(expressionCount);
            for (int i = 0; i < expressionCount; i++) {
                parsedExpressions.put(input.readUTF(), reader.readExpression());
            }
        }
    }

    public void offerExpression(String expr, Supplier<Expression> parser) {
        parsedExpressions.computeIfAbsent(expr, k -> parser.get());
    }

    public Expression getExpression(String expr) {
        return parsedExpressions.get(expr);
    }
    
    public void write(OutputStream file) throws IOException {
        try (DataOutputStream output = new DataOutputStream(file)) {
            output.writeInt(version);
            output.writeInt(parsedExpressions.size());
            ExpressionWriter writer = new ExpressionWriter(output);
            for (Map.Entry<String, Expression> entry : parsedExpressions.entrySet()) {
                output.writeUTF(entry.getKey());
                writer.writeExpression(entry.getValue());
            }
        }
    }
}
