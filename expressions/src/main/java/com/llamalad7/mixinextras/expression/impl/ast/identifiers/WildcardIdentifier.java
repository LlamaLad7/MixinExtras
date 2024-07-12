package com.llamalad7.mixinextras.expression.impl.ast.identifiers;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool;
import org.objectweb.asm.Type;

public class WildcardIdentifier implements MemberIdentifier, TypeIdentifier {
    @Override
    public boolean matches(IdentifierPool pool, FlowValue node) {
        return true;
    }

    @Override
    public boolean matches(IdentifierPool pool, Type type) {
        return true;
    }
}
