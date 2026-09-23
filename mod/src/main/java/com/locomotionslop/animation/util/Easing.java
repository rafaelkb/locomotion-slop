/*
 * Locomotion Slop - Easing
 *
 * Adapted for Locomotion Slop from the easing/timeline system of Locomotion
 * (https://github.com/Trainguy9512/Locomotion, GPLv3) by James Pelter (Trainguy9512),
 * timeline and easing system originally contributed by Marvin Schuerz.
 */
package com.locomotionslop.animation.util;

import java.util.function.FloatUnaryOperator;

/**
 * Easing curves used by keyframe interpolation and by state transitions.
 * Every curve maps 0..1 onto 0..1 and is clamped to that range before evaluation.
 */
public enum Easing {

    LINEAR(t -> t),

    SINE_IN(t -> 1.0F - (float) Math.cos((t * Math.PI) / 2.0)),
    SINE_OUT(t -> (float) Math.sin((t * Math.PI) / 2.0)),
    SINE_IN_OUT(t -> -(0.5F * ((float) Math.cos(Math.PI * t) - 1.0F))),

    QUAD_IN(t -> t * t),
    QUAD_OUT(t -> t * (2.0F - t)),
    QUAD_IN_OUT(t -> t < 0.5F ? 2.0F * t * t : -1.0F + (4.0F - 2.0F * t) * t),

    CUBIC_IN(t -> t * t * t),
    CUBIC_OUT(t -> {
        float f = t - 1.0F;
        return f * f * f + 1.0F;
    }),
    CUBIC_IN_OUT(t -> t < 0.5F ? 4.0F * t * t * t : (t - 1.0F) * (2.0F * t - 2.0F) * (2.0F * t - 2.0F) + 1.0F),

    QUART_IN(t -> t * t * t * t),
    QUART_OUT(t -> {
        float f = t - 1.0F;
        return 1.0F - f * f * f * f;
    }),
    QUART_IN_OUT(t -> t < 0.5F ? 8.0F * t * t * t * t : 1.0F - 8.0F * (t - 1.0F) * (t - 1.0F) * (t - 1.0F) * (t - 1.0F)),

    QUINT_IN(t -> t * t * t * t * t),
    QUINT_OUT(t -> {
        float f = t - 1.0F;
        return 1.0F + f * f * f * f * f;
    }),

    EXPO_IN(t -> (float) Math.pow(2.0, 10.0 * (t - 1.0))),
    EXPO_OUT(t -> 1.0F - (float) Math.pow(2.0, -10.0 * t)),

    CIRC_IN(t -> 1.0F - (float) Math.sqrt(Math.max(0.0, 1.0 - (double) (t * t)))),
    CIRC_OUT(t -> (float) Math.sqrt(Math.max(0.0, 1.0 - (double) ((t - 1.0F) * (t - 1.0F))))),

    /** Snappy ease, used for one-shot montages such as attacks and item raises. */
    SMOOTH_STEP(t -> t * t * (3.0F - 2.0F * t)),

    /** Sharper version of {@link #SMOOTH_STEP}. */
    SMOOTHER_STEP(t -> t * t * t * (t * (t * 6.0F - 15.0F) + 10.0F));

    private final FloatUnaryOperator function;

    Easing(FloatUnaryOperator function) {
        this.function = function;
    }

    public float ease(float time) {
        float clamped = time < 0.0F ? 0.0F : (Math.min(time, 1.0F));
        return this.function.applyAsFloat(clamped);
    }
}
