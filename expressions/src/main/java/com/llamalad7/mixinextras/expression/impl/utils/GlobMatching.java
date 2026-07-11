package com.llamalad7.mixinextras.expression.impl.utils;

import com.llamalad7.mixinextras.expression.impl.MatchResult;
import com.llamalad7.mixinextras.expression.impl.ast.Argument;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;

import java.util.List;

public class GlobMatching {
    public static MatchResult match(ExpressionContext ctx, FlowValue node, List<Argument> arguments, int start) {
        int m = arguments.size();
        int n = node.inputCount() - start;

        int totalEllipses = 0;
        int lastEllipsis = -1;

        for (int i = 0; i < m; i++) {
            if (arguments.get(i) == Argument.ELLIPSIS) {
                totalEllipses++;
                lastEllipsis = i;
            }
        }

        if (totalEllipses == 0) {
            if (m != n) {
                return MatchResult.FAILURE;
            }
            return suffixMatch(ctx, node, arguments, m);
        }

        int remainingEllipsisConsumptions = n - (m - totalEllipses);

        if (remainingEllipsisConsumptions < 0) {
            return MatchResult.FAILURE;
        }

        int pIdx = 0;
        int iIdx = 0;

        int backtrackEllipsisIdx = -1;
        int backtrackInputIdx = -1;
        MatchResult backtrackResult = MatchResult.SUCCESS;
        MatchResult result = MatchResult.SUCCESS;

        while (iIdx < n) {
            if (pIdx == lastEllipsis) {
                return result.then(suffixMatch(ctx, node, arguments, m - pIdx - 1));
            }
            if (arguments.get(pIdx) == Argument.ELLIPSIS) {
                // Encountered a '...', mark the fallback position
                backtrackEllipsisIdx = pIdx;
                backtrackInputIdx = iIdx;
                backtrackResult = result;
                pIdx++;
                continue;
            } else {
                // Encountered a regular predicate
                MatchResult r = ((Argument.Concrete) arguments.get(pIdx)).expression.match(node.getInput(iIdx + start), ctx);
                if (r.isSuccess()) {
                    // Predicate success; append output and move forward
                    result = result.then(r);
                    pIdx++;
                    iIdx++;
                    continue;
                }
            }
            if (backtrackEllipsisIdx != -1) {
                // Backtrack to last '...'
                if (--remainingEllipsisConsumptions < 0) {
                    return MatchResult.FAILURE;
                }
                pIdx = backtrackEllipsisIdx + 1;
                backtrackInputIdx++;
                iIdx = backtrackInputIdx;
                result = backtrackResult;
                ctx.reportArgumentMatchingFork(node, iIdx + start - 1);
            } else {
                // Predicate fail with no '...' to fall back on
                return MatchResult.FAILURE;
            }
        }

        // Must be a success
        assert arguments.stream().skip(pIdx).allMatch(arg -> arg == Argument.ELLIPSIS);

        return result;
    }

    private static MatchResult suffixMatch(ExpressionContext ctx, FlowValue node, List<Argument> arguments, int length) {
        MatchResult result = MatchResult.SUCCESS;
        for (int i = 0; i < length; i++) {
            Argument.Concrete arg = ((Argument.Concrete) arguments.get(arguments.size() - length + i));
            FlowValue input = node.getInput(node.inputCount() - length + i);
            MatchResult r = arg.expression.match(input, ctx);
            if (!r.isSuccess()) {
                return MatchResult.FAILURE;
            }
            result = result.then(r);
        }
        return result;
    }
}
