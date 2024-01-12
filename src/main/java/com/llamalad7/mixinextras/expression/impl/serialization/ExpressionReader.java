package com.llamalad7.mixinextras.expression.impl.serialization;

import com.llamalad7.mixinextras.expression.impl.ast.expressions.Expression;
import com.llamalad7.mixinextras.expression.impl.ast.identifiers.Identifier;
import com.llamalad7.mixinextras.expression.impl.ast.identifiers.PoolIdentifier;
import com.llamalad7.mixinextras.expression.impl.ast.identifiers.WildcardIdentifier;

import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class ExpressionReader {
    private final int version;
    private final DataInput input;

    public ExpressionReader(int version, DataInput input) {
        this.version = version;
        this.input = input;
    }

    public int getVersion() {
        return version;
    }

    public boolean readBoolean() throws IOException {
        return input.readBoolean();
    }

    public int readInt() throws IOException {
        return input.readInt();
    }

    public long readLong() throws IOException {
        return input.readLong();
    }

    public double readDouble() throws IOException {
        return input.readDouble();
    }

    public String readString() throws IOException {
        return input.readUTF();
    }

    public Expression readExpression() throws IOException {
        return ExpressionReading.read(this);
    }

    public List<Expression> readExpressions() throws IOException {
        int size = readInt();
        List<Expression> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            result.add(readExpression());
        }
        return result;
    }

    public Identifier readIdentifier() throws IOException {
        String str = readString();
        if (str.equals("?")) {
            return new WildcardIdentifier();
        }
        return new PoolIdentifier(str);
    }

    public <T extends Enum<T>> T readEnum(Function<String, T> valueOf) throws IOException {
        return valueOf.apply(readString());
    }
}
