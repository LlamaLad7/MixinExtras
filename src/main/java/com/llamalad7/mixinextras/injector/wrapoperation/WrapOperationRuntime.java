package com.llamalad7.mixinextras.injector.wrapoperation;

import java.util.Arrays;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class WrapOperationRuntime {
    public static void checkArgumentCount(Object[] args, int expectedArgumentCount, String expectedTypes) {
        if (args.length != expectedArgumentCount) {
            throw new IncorrectArgumentCountException(String.format(
                    "Incorrect number of arguments passed to Operation::call! Expected %s but got %s. " +
                            "Expected types were %s, actual types were %s.",
                    expectedArgumentCount, args.length,
                    expectedTypes, Arrays.stream(args).map(it -> it.getClass().getName()).collect(Collectors.joining(", ", "[", "]"))
            ));
        }
    }
}
