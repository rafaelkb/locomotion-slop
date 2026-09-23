package com.locomotionslop.mixin;

import com.locomotionslop.SlopConfig;
import com.locomotionslop.animation.animator.FirstPersonPlayerAnimator;
import com.locomotionslop.animation.joint.JointChannel;
import com.locomotionslop.animation.util.TransformSpace;
import com.locomotionslop.animation.util.TransformType;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Drives the view bob from the rig's camera joint instead of vanilla's sine-based bob.
 *
 * <p>Trainguy's first-person skeleton includes a {@code camera_jnt} whose curves carry the bob, the
 * landing dip, the swim sway and the sprint lean. Cancelling {@code bobView} and applying that
 * transform is what makes the arms and the camera feel like one object rather than two.</p>
 *
 * <p>The injection only runs while vanilla would bob anyway, so turning off the "View Bobbing"
 * option still turns off all of it.</p>
 */
@Mixin(GameRenderer.class)
public class MixinGameRenderer {

    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void slop$applyCameraJoint(PoseStack poseStack, float partialTick, CallbackInfo ci) {
        if (!SlopConfig.get().firstPersonAnimations || !SlopConfig.get().animationViewBob) {
            return;
        }
        JointChannel camera = FirstPersonPlayerAnimator.getInstance().getCameraChannel();
        if (camera == null) {
            return;
        }

        // Work on a copy: the stored channel belongs to the current frame's pose.
        JointChannel view = new JointChannel(camera);
        Vector3f rotation = view.getEulerRotationZYX();
        rotation.z *= -1;
        view.rotate(rotation, TransformSpace.LOCAL, TransformType.REPLACE);
        view.translate(view.getTranslation().mul(1.0F, 1.0F, -1.0F), TransformSpace.COMPONENT, TransformType.REPLACE);
        view.transformPoseStack(poseStack, 16.0F);

        ci.cancel();
    }
}
