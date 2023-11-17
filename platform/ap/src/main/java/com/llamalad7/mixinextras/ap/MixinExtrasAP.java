package com.llamalad7.mixinextras.ap;

import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValueInjectionInfo;
import com.llamalad7.mixinextras.injector.ModifyReceiverInjectionInfo;
import com.llamalad7.mixinextras.injector.ModifyReturnValueInjectionInfo;
import com.llamalad7.mixinextras.injector.WrapWithConditionV1InjectionInfo;
import com.llamalad7.mixinextras.injector.v2.WrapWithConditionInjectionInfo;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperationInjectionInfo;
import com.llamalad7.mixinextras.utils.info.ExtraMixinInfoSerializer;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.util.logging.MessageRouter;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.*;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MixinExtrasAP extends AbstractProcessor {
    private static final boolean MIXIN = setupMixin();

    private static boolean setupMixin() {
        try {
            // Use a simple logger until Mixin sets up its own.
            MessageRouter.setMessager(new StdoutMessager());
        } catch (NoClassDefFoundError e) {
            // Mixin AP probably isn't available, e.g. because Loom has excluded it from IDEA.
            return false;
        }
        return true;
    }

    static {
        if (MIXIN) {
            registerInjectors();
        }
    }

    private static void registerInjectors() {
        InjectionInfo.register(ModifyExpressionValueInjectionInfo.class);
        InjectionInfo.register(ModifyReceiverInjectionInfo.class);
        InjectionInfo.register(ModifyReturnValueInjectionInfo.class);
        InjectionInfo.register(WrapOperationInjectionInfo.class);
        InjectionInfo.register(WrapWithConditionV1InjectionInfo.class);
        InjectionInfo.register(WrapWithConditionInjectionInfo.class);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        processExpressions(roundEnv);
        writeInfo();
        return true;
    }

    private void processExpressions(RoundEnvironment roundEnv) {
        for (Element elem : roundEnv.getElementsAnnotatedWith(Expression.class)) {
            Expression ann = elem.getAnnotation(Expression.class);
            for (String expr : ann.value()) {
                ExtraMixinInfoWriter.offerExpression(elem, expr);
            }
        }
    }

    private void writeInfo() {
        ExtraMixinInfoWriter.build((mixin, info) -> {
            String fileName = "META-INF/mixinextras/" + mixin + ".info";
            new File(fileName).getParentFile().mkdirs();
            try {
                FileObject file = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", fileName);
                try (OutputStream fileStream = file.openOutputStream()) {
                    ExtraMixinInfoSerializer.serialize(info, fileStream);
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to write MixinExtras info file: ", e);
            }
        });
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        List<Class<? extends Annotation>> types = Arrays.asList(
                Expression.class
        );
        return types.stream().map(Class::getName).collect(Collectors.toSet());
    }
}
