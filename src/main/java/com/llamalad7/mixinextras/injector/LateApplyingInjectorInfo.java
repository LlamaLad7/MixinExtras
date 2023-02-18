package com.llamalad7.mixinextras.injector;

public interface LateApplyingInjectorInfo {
    void lateInject();

    void latePostInject();

    void wrap(LateApplyingInjectorInfo outer);
}
