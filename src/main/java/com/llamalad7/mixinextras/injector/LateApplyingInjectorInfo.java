package com.llamalad7.mixinextras.injector;

public interface LateApplyingInjectorInfo {
    void lateApply();

    void wrap(LateApplyingInjectorInfo outer);
}
