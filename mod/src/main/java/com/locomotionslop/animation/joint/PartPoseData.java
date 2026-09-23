package com.locomotionslop.animation.joint;

/**
 * A translation/rotation/scale triple in joint space.
 *
 * <p>This stands in for vanilla's {@code PartPose}: on Minecraft 1.21.1 {@code PartPose} only
 * carries position and rotation (scale support on {@code PartPose} landed in a later version),
 * while the Locomotion skeleton format stores a scale per joint. Keeping our own record means
 * the skeleton JSON can be read without losing the scale channel.</p>
 *
 * @param translation position in 1/16th blocks (model-part pixels)
 * @param rotation    euler angles in degrees, applied in ZYX order
 * @param scale       uniform or non-uniform scale multiplier
 */
public record PartPoseData(float[] translation, float[] rotation, float[] scale) {

    public static final PartPoseData IDENTITY = new PartPoseData(
            new float[]{0.0F, 0.0F, 0.0F},
            new float[]{0.0F, 0.0F, 0.0F},
            new float[]{1.0F, 1.0F, 1.0F}
    );

    public float translationX() {
        return this.translation[0];
    }

    public float translationY() {
        return this.translation[1];
    }

    public float translationZ() {
        return this.translation[2];
    }

    public float rotationX() {
        return this.rotation[0];
    }

    public float rotationY() {
        return this.rotation[1];
    }

    public float rotationZ() {
        return this.rotation[2];
    }

    public float scaleX() {
        return this.scale[0];
    }

    public float scaleY() {
        return this.scale[1];
    }

    public float scaleZ() {
        return this.scale[2];
    }

    public boolean isIdentity() {
        return this.translationX() == 0.0F && this.translationY() == 0.0F && this.translationZ() == 0.0F
                && this.rotationX() == 0.0F && this.rotationY() == 0.0F && this.rotationZ() == 0.0F
                && this.scaleX() == 1.0F && this.scaleY() == 1.0F && this.scaleZ() == 1.0F;
    }
}
