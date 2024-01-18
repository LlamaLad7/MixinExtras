package com.llamalad7.mixinextras.expression.impl;

import com.llamalad7.mixinextras.expression.grammar.ExpressionLexer;
import com.llamalad7.mixinextras.expression.grammar.ExpressionParser;
import com.llamalad7.mixinextras.expression.grammar.ExpressionParser.*;
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

    private Expression parse(StatementContext statement) {
        if (statement instanceof MemberAssignmentStatementContext) {
            return parse((MemberAssignmentStatementContext) statement);
        }
        if (statement instanceof ArrayStoreStatementContext) {
            return parse((ArrayStoreStatementContext) statement);
        }
        if (statement instanceof IdentifierAssignmentStatementContext) {
            return parse((IdentifierAssignmentStatementContext) statement);
        }
        if (statement instanceof ReturnStatementContext) {
            return parse((ReturnStatementContext) statement);
        }
        if (statement instanceof ThrowStatementContext) {
            return parse((ThrowStatementContext) statement);
        }
        if (statement instanceof ExpressionStatementContext) {
            return parse((ExpressionStatementContext) statement);
        }
        throw unimplemented();
    }

    private MemberAssignmentExpression parse(MemberAssignmentStatementContext statement) {
        return new MemberAssignmentExpression(parse(statement.receiver), parse(statement.memberName), parse(statement.value));
    }

    private ArrayStoreExpression parse(ArrayStoreStatementContext statement) {
        return new ArrayStoreExpression(parse(statement.arr), parse(statement.index), parse(statement.value));
    }

    private IdentifierAssignmentExpression parse(IdentifierAssignmentStatementContext statement) {
        return new IdentifierAssignmentExpression(parse(statement.identifier), parse(statement.value));
    }

    private ReturnExpression parse(ReturnStatementContext statement) {
        return new ReturnExpression(parse(statement.value));
    }

    private ThrowExpression parse(ThrowStatementContext statement) {
        return new ThrowExpression(parse(statement.value));
    }

    private Expression parse(ExpressionStatementContext statement) {
        return parse(statement.expression());
    }

    private Expression parse(ExpressionContext expression) {
        if (expression instanceof CapturingExpressionContext) {
            return parse((CapturingExpressionContext) expression);
        }
        if (expression instanceof ParenthesizedExpressionContext) {
            return parse((ParenthesizedExpressionContext) expression);
        }
        if (expression instanceof SuperCallExpressionContext) {
            return parse((SuperCallExpressionContext) expression);
        }
        if (expression instanceof MethodCallExpressionContext) {
            return parse((MethodCallExpressionContext) expression);
        }
        if (expression instanceof StaticMethodCallExpressionContext) {
            return parse((StaticMethodCallExpressionContext) expression);
        }
        if (expression instanceof ArrayAccessExpressionContext) {
            return parse((ArrayAccessExpressionContext) expression);
        }
        if (expression instanceof MemberAccessExpressionContext) {
            return parse((MemberAccessExpressionContext) expression);
        }
        if (expression instanceof UnaryExpressionContext) {
            return parse((UnaryExpressionContext) expression);
        }
        if (expression instanceof CastExpressionContext) {
            return parse((CastExpressionContext) expression);
        }
        if (expression instanceof InstantiationExpressionContext) {
            return parse((InstantiationExpressionContext) expression);
        }
        if (expression instanceof MultiplicativeExpressionContext) {
            return parse((MultiplicativeExpressionContext) expression);
        }
        if (expression instanceof AdditiveExpressionContext) {
            return parse((AdditiveExpressionContext) expression);
        }
        if (expression instanceof ShiftExpressionContext) {
            return parse((ShiftExpressionContext) expression);
        }
        if (expression instanceof ComparisonExpressionContext) {
            return parse((ComparisonExpressionContext) expression);
        }
        if (expression instanceof InstanceofExpressionContext) {
            return parse((InstanceofExpressionContext) expression);
        }
        if (expression instanceof EqualityExpressionContext) {
            return parse((EqualityExpressionContext) expression);
        }
        if (expression instanceof BitwiseAndExpressionContext) {
            return parse((BitwiseAndExpressionContext) expression);
        }
        if (expression instanceof BitwiseXorExpressionContext) {
            return parse((BitwiseXorExpressionContext) expression);
        }
        if (expression instanceof BitwiseOrExpressionContext) {
            return parse((BitwiseOrExpressionContext) expression);
        }
        if (expression instanceof DecimalLitExpressionContext) {
            return parse((DecimalLitExpressionContext) expression);
        }
        if (expression instanceof IntLitExpressionContext) {
            return parse((IntLitExpressionContext) expression);
        }
        if (expression instanceof StringLitExpressionContext) {
            return parse((StringLitExpressionContext) expression);
        }
        if (expression instanceof BoolLitExpressionContext) {
            return parse((BoolLitExpressionContext) expression);
        }
        if (expression instanceof NullExpressionContext) {
            return parse((NullExpressionContext) expression);
        }
        if (expression instanceof WildcardExpressionContext) {
            return parse((WildcardExpressionContext) expression);
        }
        if (expression instanceof ThisExpressionContext) {
            return parse((ThisExpressionContext) expression);
        }
        if (expression instanceof IdentifierExpressionContext) {
            return parse((IdentifierExpressionContext) expression);
        }
        throw unimplemented();
    }

    private CapturingExpression parse(CapturingExpressionContext expression) {
        this.hasExplicitCapture = true;
        return new CapturingExpression(parse(expression.expr));
    }

    private Expression parse(ParenthesizedExpressionContext expression) {
        return parse(expression.expr);
    }

    private SuperCallExpression parse(SuperCallExpressionContext expression) {
        return new SuperCallExpression(parse(expression.memberName), parse(expression.args));
    }

    private MethodCallExpression parse(MethodCallExpressionContext expression) {
        return new MethodCallExpression(parse(expression.receiver), parse(expression.memberName), parse(expression.args));
    }

    private StaticMethodCallExpression parse(StaticMethodCallExpressionContext expression) {
        return new StaticMethodCallExpression(parse(expression.memberName), parse(expression.args));
    }

    private ArrayAccessExpression parse(ArrayAccessExpressionContext expression) {
        return new ArrayAccessExpression(parse(expression.arr), parse(expression.index));
    }

    private MemberAccessExpression parse(MemberAccessExpressionContext expression) {
        return new MemberAccessExpression(parse(expression.receiver), parse(expression.memberName));
    }

    private UnaryExpression parse(UnaryExpressionContext expression) {
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

    private CastExpression parse(CastExpressionContext expression) {
        return new CastExpression(parse(expression.type), parse(expression.expr));
    }

    private InstantiationExpression parse(InstantiationExpressionContext expression) {
        return new InstantiationExpression(parse(expression.type), parse(expression.args));
    }

    private BinaryExpression parse(MultiplicativeExpressionContext expression) {
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

    private BinaryExpression parse(AdditiveExpressionContext expression) {
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

    private BinaryExpression parse(ShiftExpressionContext expression) {
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

    private ComparisonExpression parse(ComparisonExpressionContext expression) {
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

    private InstanceofExpression parse(InstanceofExpressionContext expression) {
        return new InstanceofExpression(parse(expression.expr), parse(expression.type));
    }

    private ComparisonExpression parse(EqualityExpressionContext expression) {
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

    private BinaryExpression parse(BitwiseAndExpressionContext expression) {
        return new BinaryExpression(parse(expression.left), BinaryExpression.Operator.BITWISE_AND, parse(expression.right));
    }

    private BinaryExpression parse(BitwiseXorExpressionContext expression) {
        return new BinaryExpression(parse(expression.left), BinaryExpression.Operator.BITWISE_XOR, parse(expression.right));
    }

    private BinaryExpression parse(BitwiseOrExpressionContext expression) {
        return new BinaryExpression(parse(expression.left), BinaryExpression.Operator.BITWISE_OR, parse(expression.right));
    }

    private DecimalLiteralExpression parse(DecimalLitExpressionContext expression) {
        return new DecimalLiteralExpression(Double.parseDouble(expression.getText()));
    }

    private IntLiteralExpression parse(IntLitExpressionContext expression) {
        return new IntLiteralExpression(Long.parseLong(expression.getText()));
    }

    private StringLiteralExpression parse(StringLitExpressionContext expression) {
        String text = expression.getText();
        return new StringLiteralExpression(text.substring(1, text.length() - 1));
    }

    private BooleanLiteralExpression parse(BoolLitExpressionContext expression) {
        return new BooleanLiteralExpression(Boolean.parseBoolean(expression.getText()));
    }

    private NullLiteralExpression parse(NullExpressionContext expression) {
        return new NullLiteralExpression();
    }

    private WildcardExpression parse(WildcardExpressionContext expression) {
        return new WildcardExpression();
    }

    private ThisExpression parse(ThisExpressionContext expression) {
        return new ThisExpression();
    }

    private IdentifierExpression parse(IdentifierExpressionContext expression) {
        return new IdentifierExpression(expression.getText());
    }

    private Identifier parse(NameContext name) {
        if (name instanceof IdentifierNameContext) {
            return new PoolIdentifier(name.getText());
        }
        if (name instanceof WildcardNameContext) {
            return new WildcardIdentifier();
        }
        throw unimplemented();
    }

    private List<Expression> parse(ArgumentsContext args) {
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
