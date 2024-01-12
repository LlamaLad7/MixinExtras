package com.llamalad7.mixinextras.expression.impl.serialization;

import com.llamalad7.mixinextras.expression.impl.ast.expressions.Expression;
import com.llamalad7.mixinextras.expression.impl.ast.identifiers.Identifier;
import com.llamalad7.mixinextras.expression.impl.ast.identifiers.PoolIdentifier;
import com.llamalad7.mixinextras.expression.impl.ast.identifiers.WildcardIdentifier;

import java.io.DataOutput;
import java.io.IOException;
import java.util.List;

public class ExpressionWriter {
    private final DataOutput output;

    public ExpressionWriter(DataOutput output) {
        this.output = output;
    }

    public void writeBoolean(boolean v) throws IOException {
        output.writeBoolean(v);
    }

    public void writeInt(int v) throws IOException {
        output.writeInt(v);
    }

    public void writeLong(long v) throws IOException {
        output.writeLong(v);
    }

    public void writeDouble(double v) throws IOException {
        output.writeDouble(v);
    }

    public void writeString(String s) throws IOException {
        output.writeUTF(s);
    }

    public void writeExpression(Expression expression) throws IOException {
        writeString(expression.getClass().getAnnotation(SerializedExpressionId.class).value());
        expression.write(this);
    }

    public void writeExpressions(List<Expression> expressions) throws IOException {
        writeInt(expressions.size());
        for (Expression expression : expressions) {
            writeExpression(expression);
        }
    }

    public void writeIdentifier(Identifier identifier) throws IOException {
        if (identifier instanceof WildcardIdentifier) {
            writeString("?");
        } else {
            writeString(((PoolIdentifier) identifier).name);
        }
    }

    public <T extends Enum<T>> void writeEnum(T value) throws IOException {
        writeString(value.name());
    }
}
