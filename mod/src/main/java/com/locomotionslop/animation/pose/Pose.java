/*
 * Locomotion Slop - Pose
 * Ported from Locomotion (GPLv3).
 */
package com.locomotionslop.animation.pose;

import com.locomotionslop.animation.joint.JointChannel;
import com.locomotionslop.animation.joint.skeleton.JointSkeleton;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

/**
 * A full pose: one {@link JointChannel} per joint, plus any custom attribute values.
 */
public abstract class Pose {

    protected final JointSkeleton jointSkeleton;
    protected final Map<String, JointChannel> jointChannels;
    protected final Map<String, Float> customAttributes;
    private final Map<String, Matrix4f> jointParentMatrices;

    protected Pose(JointSkeleton jointSkeleton) {
        this.jointSkeleton = jointSkeleton;
        this.jointChannels = new HashMap<>();
        this.customAttributes = jointSkeleton.getCustomAttributeDefaults();
        this.jointParentMatrices = new HashMap<>();

        for (String joint : jointSkeleton.getJoints()) {
            this.setJointChannel(joint, jointSkeleton.getJointConfiguration(joint).referencePose());
        }
    }

    protected Pose(Pose pose) {
        this.jointSkeleton = pose.jointSkeleton;
        this.jointChannels = new HashMap<>();
        pose.jointChannels.forEach((joint, channel) -> this.jointChannels.put(joint, JointChannel.of(channel)));
        this.customAttributes = new HashMap<>(pose.customAttributes);
        this.jointParentMatrices = new HashMap<>(pose.jointParentMatrices);
    }

    public JointSkeleton getJointSkeleton() {
        return this.jointSkeleton;
    }

    public void setJointChannel(String joint, JointChannel jointChannel) {
        if (this.jointSkeleton.containsJoint(joint)) {
            this.jointChannels.put(joint, jointChannel);
        }
    }

    /**
     * @return a copy of the channel for {@code joint}, or the identity channel when unknown.
     */
    public JointChannel getJointChannel(String joint) {
        return JointChannel.of(this.jointChannels.getOrDefault(joint, JointChannel.ZERO));
    }

    public void setIdentity() {
        this.jointChannels.values().forEach(JointChannel::setIdentity);
    }

    public void loadCustomAttributeValue(String name, float value) {
        this.customAttributes.put(name, value);
    }

    public float getCustomAttributeValueOrDefault(String name, float defaultValue) {
        return this.customAttributes.getOrDefault(name, defaultValue);
    }

    public void copyCustomAttributesFrom(Pose other) {
        this.customAttributes.putAll(other.customAttributes);
    }

    protected void convertChildrenJointsToComponentSpace(String parent, PoseStack poseStack) {
        JointChannel localParentJointChannel = this.getJointChannel(parent);

        poseStack.pushPose();
        poseStack.mulPose(localParentJointChannel.getTransform());

        this.getJointSkeleton().getDirectChildrenOfJoint(parent)
                .forEach(child -> this.convertChildrenJointsToComponentSpace(child, poseStack));

        Matrix4f componentSpaceMatrix = new Matrix4f(poseStack.last().pose());
        this.jointParentMatrices.put(parent, componentSpaceMatrix);
        this.setJointChannel(parent, JointChannel.of(componentSpaceMatrix, localParentJointChannel.getVisibility()));
        poseStack.popPose();
    }

    protected void convertChildrenJointsToLocalSpace(String parent, Matrix4f parentMatrix) {
        this.getJointSkeleton().getDirectChildrenOfJoint(parent)
                .forEach(child -> this.convertChildrenJointsToLocalSpace(child, this.jointParentMatrices.get(parent)));

        JointChannel parentJointChannel = this.getJointChannel(parent);
        parentJointChannel.multiply(parentMatrix.invert(new Matrix4f()), JointChannel.TransformSpace.LOCAL, JointChannel.TransformType.ADD);
        this.setJointChannel(parent, parentJointChannel);
    }
}
