package com.trainguy9512.locomotion.util;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * 1.21.1 {@link PoseStack} can multiply a quaternion, but not a full matrix.
 * Later versions added {@code mulPose(Matrix4f)}. This does the same thing and also updates normals.
 */
public final class PoseStackUtil {
    private PoseStackUtil() {
    }

    public static void multiply(PoseStack poseStack, Matrix4f transform) {
        PoseStack.Pose pose = poseStack.last();
        pose.pose().mul(transform);
        pose.normal().mul(new Matrix3f(transform));
    }
}
