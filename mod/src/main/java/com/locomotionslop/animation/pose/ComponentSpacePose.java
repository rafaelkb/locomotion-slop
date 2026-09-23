/*
 * Locomotion Slop - ComponentSpacePose
 * Ported from Locomotion (GPLv3).
 */
package com.locomotionslop.animation.pose;

import com.locomotionslop.animation.joint.JointChannel;
import com.locomotionslop.animation.joint.skeleton.JointSkeleton;
import org.joml.Matrix4f;

/**
 * A pose whose joint channels are expressed relative to the skeleton root.
 */
public class ComponentSpacePose extends Pose {

    private ComponentSpacePose(JointSkeleton jointSkeleton) {
        super(jointSkeleton);
    }

    private ComponentSpacePose(Pose pose) {
        super(pose);
    }

    public static ComponentSpacePose of(JointSkeleton jointSkeleton) {
        return new ComponentSpacePose(jointSkeleton);
    }

    public static ComponentSpacePose of(Pose pose) {
        return new ComponentSpacePose(pose);
    }

    public LocalSpacePose convertedToLocalSpace() {
        LocalSpacePose pose = LocalSpacePose.of(this);
        pose.convertChildrenJointsToLocalSpace(this.getJointSkeleton().getRootJoint(), new Matrix4f());
        return pose;
    }

    /**
     * Re-expresses joints that declare a {@code model_part_space_parent} relative to that parent,
     * which is what makes a joint channel usable as a raw {@code ModelPart} transform.
     */
    public ModelPartSpacePose convertedToModelPartSpace() {
        ModelPartSpacePose pose = ModelPartSpacePose.of(this);
        JointSkeleton skeleton = this.getJointSkeleton();

        for (String joint : skeleton.getJoints()) {
            String modelPartSpaceParent = skeleton.getJointConfiguration(joint).modelPartSpaceParent();
            if (modelPartSpaceParent != null) {
                Matrix4f invertedParentSpacePose = this.getJointChannel(modelPartSpaceParent).getTransform().invert();
                JointChannel newJointChannel = pose.getJointChannel(joint);
                newJointChannel.multiply(invertedParentSpacePose, JointChannel.TransformSpace.COMPONENT, JointChannel.TransformType.ADD);
                pose.setJointChannel(joint, newJointChannel);
            }
        }
        return pose;
    }
}
