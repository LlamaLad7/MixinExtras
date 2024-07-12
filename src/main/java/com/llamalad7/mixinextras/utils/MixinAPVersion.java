package com.llamalad7.mixinextras.utils;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.spongepowered.asm.util.VersionNumber;

import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

public class MixinAPVersion {
    private static final String MIN_VERSION = "0.8.3";
    private static final VersionNumber MIN_VERSION_NUMBER = VersionNumber.parse(MIN_VERSION);
    private static final String BOOTSTRAP_ClASS = "org/spongepowered/asm/launch/MixinBootstrap.class";
    private static boolean checked = false;

    public static void check(ProcessingEnvironment env) {
        if (checked) {
            return;
        }
        checked = true;
        try (InputStream is = findBootstrapClass()) {
            if (is == null) {
                printFailed(env);
                return;
            }
            ClassNode node = new ClassNode();
            new ClassReader(is).accept(node, ClassReader.SKIP_CODE);
            VersionNumber version = getBootstrapVersion(node);
            if (version == null) {
                printFailed(env);
                return;
            }
            if (version.compareTo(MIN_VERSION_NUMBER) < 0) {
                throw new IllegalStateException("MixinExtras requires the Mixin AP to be at least " + MIN_VERSION);
            }
        } catch (IOException ignored) {
            printFailed(env);
        }
    }

    private static VersionNumber getBootstrapVersion(ClassNode bootstrapClass) {
        for (FieldNode field : bootstrapClass.fields) {
            if (field.name.equals("VERSION")) {
                if (!(field.value instanceof String)) {
                    return null;
                }
                return VersionNumber.parse((String) field.value);
            }
        }
        return null;
    }

    private static void printFailed(ProcessingEnvironment env) {
        env.getMessager().printMessage(Diagnostic.Kind.WARNING, "[MixinExtras] Failed to determine Mixin version. Assuming >=" + MIN_VERSION);
    }

    private static InputStream findBootstrapClass() throws IOException {
        Enumeration<URL> urls = MixinAPVersion.class.getClassLoader().getResources(BOOTSTRAP_ClASS);
        if (!urls.hasMoreElements()) {
            return null;
        }
        return urls.nextElement().openStream();
    }
}
