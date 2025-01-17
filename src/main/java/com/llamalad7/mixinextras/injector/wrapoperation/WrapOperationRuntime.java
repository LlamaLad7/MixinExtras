package com.llamalad7.mixinextras.injector.wrapoperation;

import java.util.Arrays;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class WrapOperationRuntime {
    public static void checkArgumentCount(Object[] args, int expectedArgumentCount, String expectedTypes) {
        if (args.length != expectedArgumentCount) {
            // Keep the bad path in a separate method to help the JIT inline this
            throwIncorrectArgumentCount(args, expectedArgumentCount, expectedTypes);
        }
    }

    private static void throwIncorrectArgumentCount(Object[] args, int expectedArgumentCount, String expectedTypes) {
        String actualTypes = Arrays.stream(args)
                .map(it -> it == null ? "null" : it.getClass().getName())
                .collect(Collectors.joining(", ", "[", "]"));
        throw new IncorrectArgumentCountException(
                String.format(
                        "Incorrect number of arguments passed to Operation::call! Expected %s but got %s. " +
                                "Expected types were %s, actual types were %s.",
                        expectedArgumentCount, args.length,
                        expectedTypes, actualTypes
                )
        );
    }
}
