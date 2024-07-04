package com.llamalad7.mixinextras.expression.impl.ast.identifiers;

import com.llamalad7.mixinextras.expression.impl.flow.FlowValue;
import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool;

public class DefinedMemberIdentifier implements MemberIdentifier {
    public final String name;

    public DefinedMemberIdentifier(String name) {
        this.name = name;
    }

    @Override
    public boolean matches(IdentifierPool pool, FlowValue node) {
        return pool.matchesMember(name, node);
    }
}
