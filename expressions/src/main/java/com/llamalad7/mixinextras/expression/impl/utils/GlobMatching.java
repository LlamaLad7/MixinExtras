package com.llamalad7.mixinextras.expression.impl.utils;

import com.llamalad7.mixinextras.expression.impl.MatchResult;
import com.llamalad7.mixinextras.expression.impl.ast.Argument;
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class GlobMatching {
    public static MatchResult match(ExpressionContext ctx, FlowValue node, List<Argument> arguments, int start) {
        int m = arguments.size();
        int n = node.inputCount() - start;

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
                MatchResult r = ((Argument.Concrete) arguments.get(totalEllipses + i)).expression.match(node.getInput(n - k + i + start), ctx);
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
        MatchHistory backtrackHistory = initialHistory(start);
        MatchResult result = MatchResult.SUCCESS;
        MatchHistory history = backtrackHistory;

        while (iIdx < n) {
            if (pIdx < m && arguments.get(pIdx) == Argument.ELLIPSIS) {
                // Encountered a '...', mark the fallback position
                backtrackEllipsisIdx = pIdx;
                backtrackInputIdx = iIdx;
                backtrackResult = result;
                backtrackHistory = history;
                pIdx++;
                continue;
            }
            if (pIdx < m) {
                // Encountered a regular predicate
                MatchResult r = ((Argument.Concrete) arguments.get(pIdx)).expression.match(node.getInput(iIdx + start), ctx);
                if (r.isSuccess()) {
                    // Predicate success; append output and move forward
                    result = result.then(r);
                    history = new MatchHistory(history, iIdx + start);
                    pIdx++;
                    iIdx++;
                    continue;
                }
            }
            if (backtrackEllipsisIdx != -1) {
                // Backtrack to last '...'
                pIdx = backtrackEllipsisIdx + 1;
                backtrackInputIdx++;
                iIdx = backtrackInputIdx;
                result = backtrackResult;
                history = backtrackHistory;
                ctx.reportArgumentMatchingFork(node, history);
            } else {
                // Predicate fail with no '...' to fall back on
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

    private static MatchHistory initialHistory(int start) {
        MatchHistory result = null;
        for (int i = 0; i < start; i++) {
            result = new MatchHistory(result, i);
        }
        return result;
    }

    private static class MatchHistory implements Iterable<Integer> {
        private final MatchHistory previous;
        private final int matchIndex;

        public MatchHistory(MatchHistory previous, int matchIndex) {
            this.previous = previous;
            this.matchIndex = matchIndex;
        }

        @Override
        public Iterator<Integer> iterator() {
            return new Iterator<Integer>() {
                private MatchHistory current = MatchHistory.this;

                @Override
                public boolean hasNext() {
                    return current != null;
                }

                @Override
                public Integer next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    int value = current.matchIndex;
                    current = current.previous; // Move backward in history
                    return value;
                }
            };
        }
    }
}
