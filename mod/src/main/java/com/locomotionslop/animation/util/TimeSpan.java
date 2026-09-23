/*
 * Locomotion Slop - TimeSpan
 * Ported from Locomotion (GPLv3).
 */
package com.locomotionslop.animation.util;

/**
 * A length of time, stored in seconds. Minecraft runs at a fixed 20 ticks per second.
 */
public record TimeSpan(float seconds) {

    public static final float TICKS_PER_SECOND = 20.0F;

    public static TimeSpan ofSeconds(float seconds) {
        return new TimeSpan(seconds);
    }

    public static TimeSpan ofTicks(float ticks) {
        return new TimeSpan(ticks / TICKS_PER_SECOND);
    }

    public float inSeconds() {
        return this.seconds;
    }

    public float inTicks() {
        return this.seconds * TICKS_PER_SECOND;
    }

    public TimeSpan plus(TimeSpan other) {
        return new TimeSpan(this.seconds + other.seconds);
    }

    public TimeSpan scaled(float scale) {
        return new TimeSpan(this.seconds * scale);
    }

    public boolean isZero() {
        return this.seconds == 0.0F;
    }
}
