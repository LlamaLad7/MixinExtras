package com.llamalad7.mixinextras.ap;

import com.llamalad7.mixinextras.ap.expressions.ExpressionParserFacade;
import com.llamalad7.mixinextras.utils.info.ExtraMixinInfo;
import com.llamalad7.mixinextras.utils.info.ExtraMixinInfoSerializer;

import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

class ExtraMixinInfoWriter {
    private static final Map<String, ExtraMixinInfo> INFO = new HashMap<>();

    public static void build(BiConsumer<String, String> output) {
        for (Map.Entry<String, ExtraMixinInfo> entry : INFO.entrySet()) {
            output.accept(entry.getKey(), ExtraMixinInfoSerializer.serialize(entry.getValue()));
        }
        INFO.clear();
    }

    public static void offerExpression(Element elem, String expr) {
        getInfo(elem).offerExpression(expr, () -> ExpressionParserFacade.parse(expr));
    }

    private static ExtraMixinInfo getInfo(Element type) {
        return INFO.computeIfAbsent(getInternalName(getEnclosingType(type)), k -> new ExtraMixinInfo());
    }

    private static TypeElement getEnclosingType(Element elem) {
        while (!(elem instanceof TypeElement)) {
            elem = elem.getEnclosingElement();
        }
        return (TypeElement) elem;
    }

    private static String getInternalName(TypeElement element) {
        if (element == null) {
            return null;
        }
        StringBuilder reference = new StringBuilder();
        reference.append(element.getSimpleName());
        Element parent = element.getEnclosingElement();
        while (parent != null) {
            if (parent instanceof TypeElement) {
                reference.insert(0, "$").insert(0, parent.getSimpleName());
            } else if (parent instanceof PackageElement) {
                reference.insert(0, "/").insert(0, ((PackageElement)parent).getQualifiedName().toString().replace('.', '/'));
            }
            parent = parent.getEnclosingElement();
        }
        return reference.toString();
    }
}
