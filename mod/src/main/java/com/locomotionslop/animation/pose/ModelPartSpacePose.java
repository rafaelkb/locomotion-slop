/*
 * Locomotion Slop - ModelPartSpacePose
 * Ported from Locomotion (GPLv3).
 */
package com.locomotionslop.animation.pose;

import com.locomotionslop.animation.joint.skeleton.JointSkeleton;

/**
 * A pose whose channels can be handed straight to a {@code ModelPart}.
 */
public class ModelPartSpacePose extends Pose {

    protected ModelPartSpacePose(Pose pose) {
        super(pose);
    }

    static ModelPartSpacePose of(Pose pose) {
        return new ModelPartSpacePose(pose);
    }

    public static ModelPartSpacePose of(JointSkeleton jointSkeleton) {
        return new ModelPartSpacePose(ComponentSpacePose.of(jointSkeleton));
    }

    /**
     * A copy of this pose with every joint swapped for its mirror partner's mirrored transform,
     * used to play the right-hand-authored rig on a left-handed player.
     */
    public static ModelPartSpacePose ofMirrored(ModelPartSpacePose pose) {
        ModelPartSpacePose mirrored = new ModelPartSpacePose(pose);
        for (String joint : pose.getJointSkeleton().getJoints()) {
            String mirrorJoint = pose.getJointSkeleton().getJointConfiguration(joint).mirrorJoint();
            mirrored.setJointChannel(joint, pose.getJointChannel(mirrorJoint == null ? joint : mirrorJoint).mirrored());
        }
        return mirrored;
    }
}
