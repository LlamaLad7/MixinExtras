package com.llamalad7.mixinextras.injector.wrapoperation;

import com.llamalad7.mixinextras.injector.LateApplyingInjectorInfo;
import com.llamalad7.mixinextras.utils.Blackboard;
import com.llamalad7.mixinextras.utils.UniquenessHelper;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.ext.IExtension;
import org.spongepowered.asm.mixin.transformer.ext.ITargetClassContext;

import java.util.*;

/**
 * This extension is responsible for actually injecting all `@WrapOperation`s which were queued up during the normal
 * injection phase. Applying them here means we are guaranteed to run after every other injector, which is crucial.
 */
public class WrapOperationApplicatorExtension implements IExtension {
    private static final Map<ITargetClassContext, List<Runnable[]>> QUEUED_INJECTIONS =
            Blackboard.getOrPut("WrapOperation_queuedInjections", () -> Collections.synchronizedMap(new HashMap<>()));

    static void offerInjection(ITargetClassContext targetClassContext, LateApplyingInjectorInfo injectorInfo) {
        QUEUED_INJECTIONS.computeIfAbsent(targetClassContext, k -> new ArrayList<>()).add(new Runnable[]{injectorInfo::lateInject, injectorInfo::latePostInject});
    }

    @Override
    public boolean checkActive(MixinEnvironment environment) {
        return true;
    }

    @Override
    public void preApply(ITargetClassContext context) {
    }

    @Override
    public void postApply(ITargetClassContext context) {
        List<Runnable[]> queuedInjections = QUEUED_INJECTIONS.get(context);
        if (queuedInjections != null) {
            for (Runnable[] injection : queuedInjections) {
                injection[0].run();
            }
            for (Runnable[] injection : queuedInjections) {
                injection[1].run();
            }
            QUEUED_INJECTIONS.remove(context);
        }
        UniquenessHelper.clear(context.getClassNode().name);
    }

    @Override
    public void export(MixinEnvironment env, String name, boolean force, ClassNode classNode) {
    }
}
