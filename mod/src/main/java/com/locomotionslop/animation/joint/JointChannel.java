/*
 * Locomotion Slop - JointChannel
 * Ported from Locomotion (GPLv3) by James Pelter (Trainguy9512).
 * The only structural change versus upstream is that PartPose is replaced by PartPoseData,
 * because Minecraft 1.21.1's PartPose has no scale channels.
 */
package com.locomotionslop.animation.joint;

import com.locomotionslop.animation.util.Interpolator;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The animated transform of a single joint: a 4x4 matrix plus a visibility flag.
 */
public final class JointChannel {

    private final Matrix4f transform;
    private boolean visibility;

    public static final JointChannel ZERO = JointChannel.ofTranslationRotationScaleEuler(
            new Vector3f(), new Vector3f(), new Vector3f(1.0F, 1.0F, 1.0F), true
    );

    private JointChannel(Matrix4f transform, boolean visibility) {
        this.transform = transform;
        this.visibility = visibility;
    }

    public static JointChannel of(Matrix4f transform, boolean visibility) {
        return new JointChannel(new Matrix4f(transform), visibility);
    }

    public static JointChannel of(JointChannel jointChannel) {
        return JointChannel.of(jointChannel.getTransform(), jointChannel.visibility);
    }

    public static JointChannel ofPartPoseData(PartPoseData partPose) {
        return ofTranslationRotationScaleEuler(
                new Vector3f(partPose.translationX(), partPose.translationY(), partPose.translationZ()),
                new Vector3f(partPose.rotationX(), partPose.rotationY(), partPose.rotationZ()),
                new Vector3f(partPose.scaleX(), partPose.scaleY(), partPose.scaleZ()),
                true
        );
    }

    public static JointChannel ofTranslationRotationScaleEuler(Vector3f translation, Vector3f rotationEuler, Vector3f scale, boolean visibility) {
        return ofTranslationRotationScaleQuaternion(
                translation,
                new Quaternionf().rotationZYX(
                        rotationEuler.z() * DEG_TO_RAD,
                        rotationEuler.y() * DEG_TO_RAD,
                        rotationEuler.x() * DEG_TO_RAD
                ),
                scale,
                visibility
        );
    }

    public static JointChannel ofTranslationRotationScaleQuaternion(Vector3f translation, Quaternionf rotation, Vector3f scale, boolean visibility) {
        return of(new Matrix4f().translationRotateScale(translation, rotation, scale), visibility);
    }

    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    public Matrix4f getTransform() {
        return new Matrix4f(this.transform);
    }

    public boolean getVisibility() {
        return this.visibility;
    }

    public Vector3f getTranslation() {
        return this.transform.getTranslation(new Vector3f());
    }

    public Quaternionf getRotation() {
        return this.transform.getNormalizedRotation(new Quaternionf());
    }

    public Vector3f getEulerRotationZYX() {
        return this.transform.getEulerAnglesZYX(new Vector3f());
    }

    public Vector3f getScale() {
        return this.transform.getScale(new Vector3f());
    }

    public void setIdentity() {
        this.transform.identity();
    }

    public void translate(Vector3f translation, TransformSpace transformSpace, TransformType transformType) {
        switch (transformType) {
            case ADD -> {
                if (translation.x() != 0.0F || translation.y() != 0.0F || translation.z() != 0.0F) {
                    switch (transformSpace) {
                        case LOCAL -> this.transform.translate(translation);
                        case COMPONENT, PARENT -> this.transform.translateLocal(translation);
                    }
                }
            }
            case REPLACE -> this.transform.setTranslation(translation);
            case IGNORE -> {
            }
        }
    }

    public void rotate(Quaternionf rotation, TransformSpace transformSpace, TransformType transformType) {
        switch (transformType) {
            case ADD -> {
                switch (transformSpace) {
                    case LOCAL -> this.transform.rotate(rotation);
                    case COMPONENT, PARENT -> {
                        Quaternionf currentRotation = this.transform.getUnnormalizedRotation(new Quaternionf());
                        rotation.mul(currentRotation, currentRotation);
                        this.transform.translationRotateScale(this.getTranslation(), currentRotation, this.getScale());
                    }
                }
            }
            case REPLACE -> this.transform.translationRotateScale(this.getTranslation(), rotation, this.getScale());
            case IGNORE -> {
            }
        }
    }

    public void rotate(Vector3f rotationEuler, TransformSpace transformSpace, TransformType transformType) {
        this.rotate(
                new Quaternionf().rotationXYZ(
                        rotationEuler.x() * DEG_TO_RAD,
                        rotationEuler.y() * DEG_TO_RAD,
                        rotationEuler.z() * DEG_TO_RAD
                ),
                transformSpace,
                transformType
        );
    }

    public void scale(Vector3f scale, TransformSpace transformSpace, TransformType transformType) {
        switch (transformType) {
            case ADD -> {
                switch (transformSpace) {
                    case LOCAL -> this.transform.scale(scale);
                    case COMPONENT, PARENT -> this.transform.scaleLocal(scale.x(), scale.y(), scale.z());
                }
            }
            case REPLACE -> {
                Matrix4f matrix = new Matrix4f();
                matrix.translationRotateScale(this.getTranslation(), this.getRotation(), scale);
                this.transform.set(matrix);
            }
            case IGNORE -> {
            }
        }
    }

    public void multiply(JointChannel other, TransformSpace transformSpace, TransformType transformType) {
        this.multiply(other.transform, transformSpace, transformType);
    }

    public void multiply(Matrix4f transform, TransformSpace transformSpace, TransformType transformType) {
        switch (transformType) {
            case ADD -> {
                switch (transformSpace) {
                    case COMPONENT, PARENT -> this.transform.mul(transform);
                    case LOCAL -> transform.mul(this.transform, this.transform);
                }
            }
            case REPLACE -> this.transform.set(transform);
            case IGNORE -> {
            }
        }
    }

    public void invert() {
        this.transform.invert();
    }

    /**
     * Returns a copy of this channel mirrored across the X axis, used for left/right mirroring.
     */
    public JointChannel mirrored() {
        Vector3f mirroredTranslation = this.getTranslation().mul(-1.0F, 1.0F, 1.0F);
        Vector3f mirroredRotation = this.transform.getUnnormalizedRotation(new Quaternionf())
                .getEulerAnglesZYX(new Vector3f())
                .mul(1.0F, -1.0F, -1.0F);
        return JointChannel.ofTranslationRotationScaleEuler(mirroredTranslation, mirroredRotation, this.getScale(), this.visibility);
    }

    /**
     * Interpolates this channel towards {@code other} and stores the result in {@code destination}.
     */
    public JointChannel interpolate(JointChannel other, float weight, JointChannel destination) {
        Vector3f translation = this.transform.getTranslation(new Vector3f());
        Quaternionf rotation = this.transform.getUnnormalizedRotation(new Quaternionf());
        Vector3f scale = this.transform.getScale(new Vector3f());

        Vector3f otherTranslation = other.transform.getTranslation(new Vector3f());
        Quaternionf otherRotation = other.transform.getUnnormalizedRotation(new Quaternionf());
        Vector3f otherScale = other.transform.getScale(new Vector3f());

        translation.lerp(otherTranslation, weight);
        rotation.slerp(otherRotation, weight);
        scale.lerp(otherScale, weight);
        boolean visibility = Interpolator.BOOLEAN_BLEND.interpolate(this.visibility, other.visibility, weight);

        destination.transform.translationRotateScale(translation, rotation, scale);
        destination.visibility = visibility;
        return destination;
    }

    public JointChannel interpolate(JointChannel other, float weight) {
        return this.interpolate(other, weight, this);
    }

    /**
     * Applies this channel to a pose stack. Translations are stored in model-part pixels, so they
     * are divided by {@code transformMultiplier} (16 for model parts) to get block-space units.
     */
    public void transformPoseStack(PoseStack poseStack, float transformMultiplier) {
        Matrix4f matrix = new Matrix4f(this.transform);
        poseStack.mulPose(matrix.setTranslation(this.getTranslation().div(transformMultiplier)));
    }

    public void transformPoseStack(PoseStack poseStack) {
        this.transformPoseStack(poseStack, 1.0F);
    }

    public enum TransformSpace {
        COMPONENT,
        PARENT,
        LOCAL
    }

    public enum TransformType {
        IGNORE,
        REPLACE,
        ADD
    }
}
