/*
 * Locomotion Slop - Interpolator
 * Ported from Locomotion (GPLv3); original author Marvin Schuerz.
 */
package com.locomotionslop.animation.util;

import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Strategy for blending between two values of the same type.
 */
@FunctionalInterface
public interface Interpolator<T> {

    T interpolate(T a, T b, float time);

    static <T> Interpolator<T> constant() {
        return (a, b, time) -> b;
    }

    Interpolator<Float> FLOAT = (a, b, time) -> a + (b - a) * time;

    Interpolator<Boolean> BOOLEAN_KEYFRAME = constant();

    Interpolator<Boolean> BOOLEAN_BLEND = (a, b, time) -> time < 0.5F ? a : b;

    Interpolator<Vector3f> VECTOR_FLOAT = (a, b, time) -> {
        if (time == 0.0F) {
            return new Vector3f(a);
        }
        if (time == 1.0F) {
            return new Vector3f(b);
        }
        return a.lerp(b, time, new Vector3f());
    };

    Interpolator<Quaternionf> QUATERNION = (a, b, time) -> {
        if (time == 0.0F) {
            return new Quaternionf(a);
        }
        if (time == 1.0F) {
            return new Quaternionf(b);
        }
        return a.slerp(b, time, new Quaternionf());
    };
}
