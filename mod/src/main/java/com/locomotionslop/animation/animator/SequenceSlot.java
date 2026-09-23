package com.locomotionslop.animation.animator;

import com.locomotionslop.animation.joint.skeleton.JointSkeleton;
import com.locomotionslop.animation.pose.LocalSpacePose;
import com.locomotionslop.animation.sequence.AnimationSequence;
import com.locomotionslop.resource.SlopResources;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Playback state for one animation sequence: where it is, how fast it runs, and whether it loops.
 */
public final class SequenceSlot {

    private ResourceLocation sequence;
    private float time;
    private float rate = 1.0F;
    private boolean looping;
    private float length = 0.0F;

    public SequenceSlot() {
        this.sequence = null;
    }

    public void start(ResourceLocation sequence, boolean looping) {
        this.start(sequence, looping, 1.0F);
    }

    public void start(ResourceLocation sequence, boolean looping, float rate) {
        boolean restarting = this.sequence == null || !this.sequence.equals(sequence);
        this.sequence = sequence;
        this.looping = looping;
        this.rate = rate;
        if (restarting) {
            this.time = 0.0F;
        }
        AnimationSequence loaded = SlopResources.getAnimationSequenceOrNull(sequence);
        this.length = loaded == null ? 0.0F : loaded.length().inSeconds();
    }

    public void setTime(float time) {
        this.time = Math.max(0.0F, time);
    }

    public void setRate(float rate) {
        this.rate = rate;
    }

    public void advance(float deltaSeconds) {
        if (this.sequence == null) {
            return;
        }
        this.time += deltaSeconds * this.rate;
        if (this.looping && this.length > 0.0F) {
            this.time %= this.length;
            if (this.time < 0.0F) {
                this.time += this.length;
            }
        } else if (this.length > 0.0F && this.time > this.length) {
            this.time = this.length;
        }
    }

    public boolean isPlaying() {
        return this.sequence != null;
    }

    public boolean isFinished() {
        return this.sequence == null || (!this.looping && this.length > 0.0F && this.time >= this.length);
    }

    public float progress() {
        return this.length <= 0.0F ? 1.0F : Math.min(1.0F, this.time / this.length);
    }

    public float time() {
        return this.time;
    }

    public float length() {
        return this.length;
    }

    @Nullable
    public ResourceLocation sequence() {
        return this.sequence;
    }

    /**
     * Samples this slot. Returns null when the sequence is missing from the loaded resources, so a
     * pack that removes a file degrades to "no animation" instead of crashing.
     */
    @Nullable
    public LocalSpacePose sample(JointSkeleton skeleton, float timeOffsetSeconds) {
        if (this.sequence == null) {
            return null;
        }
        AnimationSequence loaded = SlopResources.getAnimationSequenceOrNull(this.sequence);
        if (loaded == null) {
            return null;
        }
        return loaded.samplePose(skeleton, this.time + timeOffsetSeconds, this.looping);
    }

    public void clear() {
        this.sequence = null;
        this.time = 0.0F;
        this.length = 0.0F;
    }
}
