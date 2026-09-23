/*
 * Locomotion Slop - LocalSpacePose
 * Ported from Locomotion (GPLv3); blend profiles replaced by a simple per-joint mask.
 */
package com.locomotionslop.animation.pose;

import com.locomotionslop.animation.joint.JointChannel;
import com.locomotionslop.animation.joint.skeleton.JointMask;
import com.locomotionslop.animation.joint.skeleton.JointSkeleton;
import com.locomotionslop.animation.util.Easing;
import com.locomotionslop.animation.util.Interpolator;
import org.jetbrains.annotations.Nullable;

/**
 * A pose whose joint channels are expressed relative to their parent joint.
 */
public class LocalSpacePose extends Pose {

    private LocalSpacePose(JointSkeleton jointSkeleton) {
        super(jointSkeleton);
    }

    private LocalSpacePose(Pose pose) {
        super(pose);
    }

    public static LocalSpacePose of(JointSkeleton jointSkeleton) {
        return new LocalSpacePose(jointSkeleton);
    }

    public static LocalSpacePose of(Pose pose) {
        return new LocalSpacePose(pose);
    }

    public ComponentSpacePose convertedToComponentSpace() {
        ComponentSpacePose pose = ComponentSpacePose.of(this);
        pose.convertChildrenJointsToComponentSpace(this.getJointSkeleton().getRootJoint(), new com.mojang.blaze3d.vertex.PoseStack());
        return pose;
    }

    public LocalSpacePose mirrored() {
        LocalSpacePose mirroredPose = new LocalSpacePose(this);
        for (String joint : this.jointChannels.keySet()) {
            JointSkeleton.JointConfiguration configuration = this.getJointSkeleton().getJointConfiguration(joint);
            String mirrorJoint = configuration.mirrorJoint() != null ? configuration.mirrorJoint() : joint;
            mirroredPose.setJointChannel(joint, this.getJointChannel(mirrorJoint).mirrored());
        }
        return mirroredPose;
    }

    /**
     * Blends {@code other} into this pose in place and returns it.
     */
    public LocalSpacePose interpolated(LocalSpacePose other, float weight) {
        return this.interpolated(other, weight, null, this);
    }

    public LocalSpacePose interpolated(LocalSpacePose other, float weight, @Nullable JointMask blendMask) {
        return this.interpolated(other, weight, blendMask, this);
    }

    public LocalSpacePose interpolated(LocalSpacePose other, float weight, LocalSpacePose destination) {
        return this.interpolated(other, weight, null, destination);
    }

    /**
     * Blends {@code other} into {@code destination}, which must start out holding the pose to blend from.
     *
     * @param weight     0 keeps the destination pose, 1 replaces it with {@code other}
     * @param blendMask  optional per-joint weights
     */
    public LocalSpacePose interpolated(LocalSpacePose other, float weight, @Nullable JointMask blendMask, LocalSpacePose destination) {
        if (weight == 0.0F) {
            return destination;
        }
        for (String customAttribute : this.jointSkeleton.getCustomAttributes()) {
            float attributeWeight = blendMask == null ? weight : weight * blendMask.customAttributeWeight(customAttribute);
            float from = destination.customAttributes.getOrDefault(customAttribute, 0.0F);
            float to = other.customAttributes.getOrDefault(customAttribute, 0.0F);
            destination.customAttributes.put(customAttribute, Interpolator.FLOAT.interpolate(from, to, attributeWeight));
        }
        for (String joint : this.jointSkeleton.getJoints()) {
            float jointWeight = blendMask == null ? weight : weight * blendMask.jointWeight(joint);
            if (jointWeight <= 0.0F) {
                continue;
            }
            JointChannel destinationChannel = destination.getJointChannel(joint);
            if (jointWeight == 1.0F) {
                destination.setJointChannel(joint, other.getJointChannel(joint));
            } else {
                destination.setJointChannel(joint, destinationChannel.interpolate(other.getJointChannel(joint), jointWeight));
            }
        }
        return destination;
    }

    /**
     * Blend driven by an eased transition, used for state cross-fades.
     *
     * @param time 0..1 progress of the transition
     */
    public LocalSpacePose interpolatedByTransition(LocalSpacePose other, float time, Easing easing, @Nullable JointMask blendMask, LocalSpacePose destination) {
        float easedTime = easing.ease(time);
        return this.interpolated(other, easedTime, blendMask, destination);
    }

    public void multiply(LocalSpacePose other, JointChannel.TransformSpace transformSpace) {
        this.jointChannels.forEach((joint, channel) ->
                channel.multiply(other.jointChannels.get(joint), transformSpace, JointChannel.TransformType.ADD));
    }

    public void invert() {
        this.jointChannels.values().forEach(JointChannel::invert);
    }
}
