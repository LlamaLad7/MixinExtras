package com.llamalad7.mixinextras.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Allows us to retrieve information about our own package and those from all other relocated versions.
 */
public class PackageUtils {
    private static final Set<String> PACKAGES = Blackboard.getOrPut("MixinExtras_Packages", HashSet::new);
    private static final String PACKAGE = StringUtils.removeEnd(PackageUtils.class.getName(), ".utils.PackageUtils");

    public static void init() {
        PACKAGES.add(PACKAGE);
    }

    public static String getPackage() {
        return PACKAGE;
    }

    public static Set<String> getAllClassNames(String ourName) {
        String ourBinaryName = ourName.replace('/', '.');
        return PACKAGES.stream().map(it -> StringUtils.replaceOnce(ourBinaryName, PACKAGE, it)).collect(Collectors.toSet());
    }
}
