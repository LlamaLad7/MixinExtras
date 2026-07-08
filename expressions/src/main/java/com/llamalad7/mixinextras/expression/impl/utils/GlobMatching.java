package com.llamalad7.mixinextras.expression.impl.utils;

import com.llamalad7.mixinextras.expression.impl.MatchResult;
import com.llamalad7.mixinextras.expression.impl.ast.Argument;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;

import java.util.List;

public class GlobMatching {
    public static MatchResult match(ExpressionContext ctx, FlowValue node, List<Argument> arguments) {
        int m = arguments.size();
        int n = node.inputCount();

        int totalEllipses = 0;
        int firstEllipsis = -1;
        int lastEllipsis = -1;

        for (int i = 0; i < m; i++) {
            if (arguments.get(i) == Argument.ELLIPSIS) {
                totalEllipses++;
                if (firstEllipsis == -1) {
                    firstEllipsis = i;
                }
                lastEllipsis = i;
            }
        }

        // ---------------------------------------------------------
        // SPECIAL CASE: '...'s at the start and nowhere else (Suffix Matching)
        // ---------------------------------------------------------
        if (firstEllipsis == 0 && totalEllipses == lastEllipsis + 1) {
            int k = m - totalEllipses; // Number of predicates at the end
            if (n < k) {
                return MatchResult.FAILURE;
            }

            MatchResult result = MatchResult.SUCCESS;
            for (int i = 0; i < k; i++) {
                MatchResult r = ((Argument.Concrete) arguments.get(totalEllipses + i)).expression.match(node.getInput(n - k + i), ctx);
                if (!r.isSuccess()) {
                    return MatchResult.FAILURE;
                }
                result = result.then(r);
            }
            return result;
        }

        // ---------------------------------------------------------
        // GENERAL CASE: O(m * n) Time, O(1) Auxiliary Space
        // ---------------------------------------------------------
        int pIdx = 0;
        int iIdx = 0;

        int backtrackEllipsisIdx = -1;
        int backtrackInputIdx = -1;
        MatchResult backtrackResult = MatchResult.SUCCESS;
        MatchResult result = MatchResult.SUCCESS;

        while (iIdx < n) {
            if (pIdx < m && arguments.get(pIdx) == Argument.ELLIPSIS) {
                // Encountered a '...', mark the fallback position
                backtrackEllipsisIdx = pIdx;
                backtrackInputIdx = iIdx;
                backtrackResult = result;
                pIdx++;
            } else if (pIdx < m) {
                // Encountered a regular predicate
                MatchResult r = ((Argument.Concrete) arguments.get(pIdx)).expression.match(node.getInput(iIdx), ctx);
                if (r.isSuccess()) {
                    // Predicate success; append output and move forward
                    result = result.then(r);
                    pIdx++;
                    iIdx++;
                } else if (backtrackEllipsisIdx != -1) {
                    // Predicate fail; revert execution to the last seen '...'
                    pIdx = backtrackEllipsisIdx + 1;
                    backtrackInputIdx++;      // Force the '...' to consume 1 more element
                    iIdx = backtrackInputIdx;
                    result = backtrackResult;     // Abandon allocations done since the last '...'
                } else {
                    // Predicate fail with no '...' to fall back on
                    return MatchResult.FAILURE;
                }
            } else if (backtrackEllipsisIdx != -1) {
                // Exhausted pattern but string isn't finished; revert execution to last '...'
                pIdx = backtrackEllipsisIdx + 1;
                backtrackInputIdx++;
                iIdx = backtrackInputIdx;
                result = backtrackResult;
            } else {
                return MatchResult.FAILURE;
            }
        }

        // Consume any lingering '...'s at the end of the pattern
        while (pIdx < m && arguments.get(pIdx) == Argument.ELLIPSIS) {
            pIdx++;
        }

        // Pattern successfully mapped entirely to the input?
        if (pIdx == m) {
            return result;
        } else {
            return MatchResult.FAILURE;
        }
    }
}
