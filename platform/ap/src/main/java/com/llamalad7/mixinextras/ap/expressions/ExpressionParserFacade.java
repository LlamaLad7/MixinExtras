package com.llamalad7.mixinextras.ap.expressions;

import com.llamalad7.mixinextras.ap.grammar.ExpressionLexer;
import com.llamalad7.mixinextras.ap.grammar.ExpressionParser;
import com.llamalad7.mixinextras.expression.impl.ast.expressions.*;
import com.llamalad7.mixinextras.expression.impl.ast.identifiers.Identifier;
import com.llamalad7.mixinextras.expression.impl.ast.identifiers.PoolIdentifier;
import com.llamalad7.mixinextras.expression.impl.ast.identifiers.WildcardIdentifier;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;

import java.util.BitSet;
import java.util.List;
import java.util.stream.Collectors;

public class ExpressionParserFacade {
    private boolean hasExplicitCapture = false;

    public static Expression parse(String input) {
        ExpressionLexer lexer = new ExpressionLexer(CharStreams.fromString(input));
        setupErrorListeners(lexer, input);
        ExpressionParser parser = new ExpressionParser(new CommonTokenStream(lexer));
        setupErrorListeners(parser, input);
        ExpressionParserFacade facade = new ExpressionParserFacade();
        Expression parsed = facade.parse(parser.root().statement());
        if (facade.hasExplicitCapture) {
            return parsed;
        }
        return new CapturingExpression(parsed);
    }

    private Expression parse(ExpressionParser.StatementContext statement) {
        if (statement instanceof ExpressionParser.MemberAssignmentStatementContext) {
            return parse((ExpressionParser.MemberAssignmentStatementContext) statement);
        }
        if (statement instanceof ExpressionParser.ArrayStoreStatementContext) {
            return parse((ExpressionParser.ArrayStoreStatementContext) statement);
        }
        if (statement instanceof ExpressionParser.IdentifierAssignmentContext) {
            return parse((ExpressionParser.IdentifierAssignmentContext) statement);
        }
        if (statement instanceof ExpressionParser.ExpressionStatementContext) {
            return parse(((ExpressionParser.ExpressionStatementContext) statement));
        }
        throw unimplemented();
    }

    private MemberAssignmentExpression parse(ExpressionParser.MemberAssignmentStatementContext statement) {
        return new MemberAssignmentExpression(parse(statement.receiver), parse(statement.memberName), parse(statement.value));
    }

    private ArrayStoreExpression parse(ExpressionParser.ArrayStoreStatementContext statement) {
        return new ArrayStoreExpression(parse(statement.arr), parse(statement.index), parse(statement.value));
    }

    private IdentifierAssignmentExpression parse(ExpressionParser.IdentifierAssignmentContext statement) {
        return new IdentifierAssignmentExpression(parse(statement.identifier), parse(statement.value));
    }

    private Expression parse(ExpressionParser.ExpressionStatementContext statement) {
        return parse(statement.expression());
    }

    private Expression parse(ExpressionParser.ExpressionContext expression) {
        if (expression instanceof ExpressionParser.CapturingExpressionContext) {
            return parse((ExpressionParser.CapturingExpressionContext) expression);
        }
        if (expression instanceof ExpressionParser.ParenthesizedExpressionContext) {
            return parse((ExpressionParser.ParenthesizedExpressionContext) expression);
        }
        if (expression instanceof ExpressionParser.MethodCallExpressionContext) {
            return parse((ExpressionParser.MethodCallExpressionContext) expression);
        }
        if (expression instanceof ExpressionParser.StaticMethodCallExpressionContext) {
            return parse((ExpressionParser.StaticMethodCallExpressionContext) expression);
        }
        if (expression instanceof ExpressionParser.ArrayAccessExpressionContext) {
            return parse((ExpressionParser.ArrayAccessExpressionContext) expression);
        }
        if (expression instanceof ExpressionParser.MemberAccessExpressionContext) {
            return parse((ExpressionParser.MemberAccessExpressionContext) expression);
        }
        if (expression instanceof ExpressionParser.UnaryExpressionContext) {
            return parse((ExpressionParser.UnaryExpressionContext) expression);
        }
        if (expression instanceof ExpressionParser.CastExpressionContext) {
            return parse((ExpressionParser.CastExpressionContext) expression);
        }
        if (expression instanceof ExpressionParser.InstantiationExpressionContext) {
            return parse((ExpressionParser.InstantiationExpressionContext) expression);
        }
        if (expression instanceof ExpressionParser.MultiplicativeExpressionContext) {
            return parse((ExpressionParser.MultiplicativeExpressionContext) expression);
        }
        if (expression instanceof ExpressionParser.AdditiveExpressionContext) {
            return parse((ExpressionParser.AdditiveExpressionContext) expression);
        }
        if (expression instanceof ExpressionParser.ShiftExpressionContext) {
            return parse((ExpressionParser.ShiftExpressionContext) expression);
        }
        if (expression instanceof ExpressionParser.ComparisonExpressionContext) {
            return parse((ExpressionParser.ComparisonExpressionContext) expression);
        }
        if (expression instanceof ExpressionParser.InstanceofExpressionContext) {
            return parse((ExpressionParser.InstanceofExpressionContext) expression);
        }
        if (expression instanceof ExpressionParser.EqualityExpressionContext) {
            return parse((ExpressionParser.EqualityExpressionContext) expression);
        }
        if (expression instanceof ExpressionParser.BitwiseAndExpressionContext) {
            return parse((ExpressionParser.BitwiseAndExpressionContext) expression);
        }
        if (expression instanceof ExpressionParser.BitwiseXorExpressionContext) {
            return parse((ExpressionParser.BitwiseXorExpressionContext) expression);
        }
        if (expression instanceof ExpressionParser.BitwiseOrExpressionContext) {
            return parse((ExpressionParser.BitwiseOrExpressionContext) expression);
        }
        if (expression instanceof ExpressionParser.DecimalLitExpressionContext) {
            return parse((ExpressionParser.DecimalLitExpressionContext) expression);
        }
        if (expression instanceof ExpressionParser.IntLitExpressionContext) {
            return parse((ExpressionParser.IntLitExpressionContext) expression);
        }
        if (expression instanceof ExpressionParser.StringLitExpressionContext) {
            return parse((ExpressionParser.StringLitExpressionContext) expression);
        }
        if (expression instanceof ExpressionParser.BoolLitExpressionContext) {
            return parse((ExpressionParser.BoolLitExpressionContext) expression);
        }
        if (expression instanceof ExpressionParser.NullExpressionContext) {
            return parse((ExpressionParser.NullExpressionContext) expression);
        }
        if (expression instanceof ExpressionParser.WildcardExpressionContext) {
            return parse((ExpressionParser.WildcardExpressionContext) expression);
        }
        if (expression instanceof ExpressionParser.IdentifierExpressionContext) {
            return parse((ExpressionParser.IdentifierExpressionContext) expression);
        }
        throw unimplemented();
    }

    private CapturingExpression parse(ExpressionParser.CapturingExpressionContext expression) {
        this.hasExplicitCapture = true;
        return new CapturingExpression(parse(expression.expr));
    }

    private Expression parse(ExpressionParser.ParenthesizedExpressionContext expression) {
        return parse(expression.expr);
    }

    private MethodCallExpression parse(ExpressionParser.MethodCallExpressionContext expression) {
        return new MethodCallExpression(parse(expression.receiver), parse(expression.memberName), parse(expression.args));
    }

    private StaticMethodCallExpression parse(ExpressionParser.StaticMethodCallExpressionContext expression) {
        return new StaticMethodCallExpression(parse(expression.memberName), parse(expression.args));
    }

    private ArrayAccessExpression parse(ExpressionParser.ArrayAccessExpressionContext expression) {
        return new ArrayAccessExpression(parse(expression.arr), parse(expression.index));
    }

    private MemberAccessExpression parse(ExpressionParser.MemberAccessExpressionContext expression) {
        return new MemberAccessExpression(parse(expression.receiver), parse(expression.memberName));
    }

    private UnaryExpression parse(ExpressionParser.UnaryExpressionContext expression) {
        UnaryExpression.Operator op;
        switch (expression.op.getType()) {
            case ExpressionLexer.Minus:
                op = UnaryExpression.Operator.MINUS;
                break;
            case ExpressionLexer.BitwiseNot:
                op = UnaryExpression.Operator.BITWISE_NOT;
                break;
            default:
                throw unimplemented();
        }
        return new UnaryExpression(op, parse(expression.expr));
    }

    private CastExpression parse(ExpressionParser.CastExpressionContext expression) {
        return new CastExpression(parse(expression.type), parse(expression.expr));
    }

    private InstantiationExpression parse(ExpressionParser.InstantiationExpressionContext expression) {
        return new InstantiationExpression(parse(expression.type), parse(expression.args));
    }

    private BinaryExpression parse(ExpressionParser.MultiplicativeExpressionContext expression) {
        BinaryExpression.Operator op;
        switch (expression.op.getType()) {
            case ExpressionLexer.Mult:
                op = BinaryExpression.Operator.MULT;
                break;
            case ExpressionLexer.Div:
                op = BinaryExpression.Operator.DIV;
                break;
            case ExpressionLexer.Mod:
                op = BinaryExpression.Operator.MOD;
                break;
            default:
                throw unimplemented();
        }
        return new BinaryExpression(parse(expression.left), op, parse(expression.right));
    }

    private BinaryExpression parse(ExpressionParser.AdditiveExpressionContext expression) {
        BinaryExpression.Operator op;
        switch (expression.op.getType()) {
            case ExpressionLexer.Plus:
                op = BinaryExpression.Operator.PLUS;
                break;
            case ExpressionLexer.Minus:
                op = BinaryExpression.Operator.MINUS;
                break;
            default:
                throw unimplemented();
        }
        return new BinaryExpression(parse(expression.left), op, parse(expression.right));
    }

    private BinaryExpression parse(ExpressionParser.ShiftExpressionContext expression) {
        BinaryExpression.Operator op;
        switch (expression.op.getType()) {
            case ExpressionLexer.Shl:
                op = BinaryExpression.Operator.SHL;
                break;
            case ExpressionLexer.Shr:
                op = BinaryExpression.Operator.SHR;
                break;
            case ExpressionLexer.Ushr:
                op = BinaryExpression.Operator.USHR;
                break;
            default:
                throw unimplemented();
        }
        return new BinaryExpression(parse(expression.left), op, parse(expression.right));
    }

    private ComparisonExpression parse(ExpressionParser.ComparisonExpressionContext expression) {
        ComparisonExpression.Operator op;
        switch (expression.op.getType()) {
            case ExpressionLexer.Lt:
                op = ComparisonExpression.Operator.LT;
                break;
            case ExpressionLexer.Le:
                op = ComparisonExpression.Operator.LE;
                break;
            case ExpressionLexer.Gt:
                op = ComparisonExpression.Operator.GT;
                break;
            case ExpressionLexer.Ge:
                op = ComparisonExpression.Operator.GE;
                break;
            default:
                throw unimplemented();
        }
        return new ComparisonExpression(parse(expression.left), op, parse(expression.right));
    }

    private InstanceofExpression parse(ExpressionParser.InstanceofExpressionContext expression) {
        return new InstanceofExpression(parse(expression.expr), parse(expression.type));
    }

    private ComparisonExpression parse(ExpressionParser.EqualityExpressionContext expression) {
        ComparisonExpression.Operator op;
        switch (expression.op.getType()) {
            case ExpressionLexer.Eq:
                op = ComparisonExpression.Operator.EQ;
                break;
            case ExpressionLexer.Ne:
                op = ComparisonExpression.Operator.NE;
                break;
            default:
                throw unimplemented();
        }
        return new ComparisonExpression(parse(expression.left), op, parse(expression.right));
    }

    private BinaryExpression parse(ExpressionParser.BitwiseAndExpressionContext expression) {
        return new BinaryExpression(parse(expression.left), BinaryExpression.Operator.BITWISE_AND, parse(expression.right));
    }

    private BinaryExpression parse(ExpressionParser.BitwiseXorExpressionContext expression) {
        return new BinaryExpression(parse(expression.left), BinaryExpression.Operator.BITWISE_XOR, parse(expression.right));
    }

    private BinaryExpression parse(ExpressionParser.BitwiseOrExpressionContext expression) {
        return new BinaryExpression(parse(expression.left), BinaryExpression.Operator.BITWISE_OR, parse(expression.right));
    }

    private DecimalLiteralExpression parse(ExpressionParser.DecimalLitExpressionContext expression) {
        return new DecimalLiteralExpression(Double.parseDouble(expression.getText()));
    }

    private IntLiteralExpression parse(ExpressionParser.IntLitExpressionContext expression) {
        return new IntLiteralExpression(Long.parseLong(expression.getText()));
    }

    private StringLiteralExpression parse(ExpressionParser.StringLitExpressionContext expression) {
        String text = expression.getText();
        return new StringLiteralExpression(text.substring(1, text.length() - 1));
    }

    private BooleanLiteralExpression parse(ExpressionParser.BoolLitExpressionContext expression) {
        return new BooleanLiteralExpression(Boolean.parseBoolean(expression.getText()));
    }

    private NullLiteralExpression parse(ExpressionParser.NullExpressionContext expression) {
        return new NullLiteralExpression();
    }

    private WildcardExpression parse(ExpressionParser.WildcardExpressionContext expression) {
        return new WildcardExpression();
    }

    private IdentifierExpression parse(ExpressionParser.IdentifierExpressionContext expression) {
        return new IdentifierExpression(expression.getText());
    }

    private Identifier parse(ExpressionParser.NameContext name) {
        if (name instanceof ExpressionParser.IdentifierNameContext) {
            return new PoolIdentifier(name.getText());
        }
        if (name instanceof ExpressionParser.WildcardNameContext) {
            return new WildcardIdentifier();
        }
        throw unimplemented();
    }

    private List<Expression> parse(ExpressionParser.ArgumentsContext args) {
        return args.expression().stream().map(this::parse).collect(Collectors.toList());
    }

    private RuntimeException unimplemented() {
        return new IllegalStateException("Unimplemented parser element!");
    }

    private static void setupErrorListeners(Recognizer<?, ?> recognizer, String expr) {
        recognizer.removeErrorListeners();
        recognizer.addErrorListener(new ANTLRErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
                throw new RuntimeException(
                        String.format(
                                "Failed to parse expression \"%s\": line %s:%s: %s",
                                expr, line, charPositionInLine, msg
                        )
                );
            }

            @Override
            public void reportAmbiguity(Parser recognizer, DFA dfa, int startIndex, int stopIndex, boolean exact, BitSet ambigAlts, ATNConfigSet configs) {
            }

            @Override
            public void reportAttemptingFullContext(Parser recognizer, DFA dfa, int startIndex, int stopIndex, BitSet conflictingAlts, ATNConfigSet configs) {
            }

            @Override
            public void reportContextSensitivity(Parser recognizer, DFA dfa, int startIndex, int stopIndex, int prediction, ATNConfigSet configs) {
            }
        });
    }
}
