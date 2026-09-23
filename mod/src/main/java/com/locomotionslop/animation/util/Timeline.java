/*
 * Locomotion Slop - Timeline
 * Ported from Locomotion (GPLv3); original author Marvin Schuerz.
 */
package com.locomotionslop.animation.util;

import java.util.Map;
import java.util.TreeMap;

/**
 * An ordered set of keyframes for a single channel, sampled by time in seconds.
 */
public class Timeline<T> {

    private final TreeMap<Float, Keyframe<T>> keyframes;
    private final Interpolator<T> interpolator;
    private final float length;

    private Timeline(Interpolator<T> interpolator, float length) {
        this.keyframes = new TreeMap<>();
        this.interpolator = interpolator;
        this.length = length;
    }

    public static <T> Timeline<T> of(Interpolator<T> interpolator, float length) {
        return new Timeline<>(interpolator, length);
    }

    public float getLength() {
        return this.length;
    }

    public boolean isEmpty() {
        return this.keyframes.isEmpty();
    }

    public T getValueAtTime(float time, boolean looping) {
        return looping ? this.getValueAtTimeLooped(time) : this.getValueAtTime(time);
    }

    public T getValueAtTime(float time) {
        Map.Entry<Float, Keyframe<T>> firstKeyframe = this.keyframes.floorEntry(time);
        Map.Entry<Float, Keyframe<T>> secondKeyframe = this.keyframes.ceilingEntry(time);

        if (firstKeyframe == null) {
            return secondKeyframe.getValue().value();
        }
        if (secondKeyframe == null) {
            return firstKeyframe.getValue().value();
        }
        if (firstKeyframe.getKey().equals(secondKeyframe.getKey())) {
            return firstKeyframe.getValue().value();
        }

        float relativeTime = (time - firstKeyframe.getKey()) / (secondKeyframe.getKey() - firstKeyframe.getKey());
        return this.interpolator.interpolate(
                firstKeyframe.getValue().value(),
                secondKeyframe.getValue().value(),
                secondKeyframe.getValue().easing().ease(relativeTime)
        );
    }

    public T getValueAtTimeLooped(float time) {
        if (this.length <= 0.0F) {
            return this.getValueAtTime(time);
        }
        float wrapped = time % this.length;
        if (wrapped < 0.0F) {
            wrapped += this.length;
        }
        return this.getValueAtTime(wrapped);
    }

    public Timeline<T> addKeyframe(float time, T value) {
        return this.addKeyframe(time, value, Easing.LINEAR);
    }

    public Timeline<T> addKeyframe(float time, T value, Easing easing) {
        this.keyframes.put(time, new Keyframe<>(value, easing));
        return this;
    }

    public record Keyframe<T>(T value, Easing easing) {
    }
}
