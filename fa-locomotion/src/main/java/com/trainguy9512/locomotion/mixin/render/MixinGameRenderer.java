package com.trainguy9512.locomotion.mixin.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.trainguy9512.locomotion.LocomotionMain;
import com.trainguy9512.locomotion.animation.animator.JointAnimatorDispatcher;
import com.trainguy9512.locomotion.animation.animator.JointAnimatorRegistry;
import com.trainguy9512.locomotion.render.FirstPersonPlayerRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {
    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void computePosePriorToRendering(DeltaTracker deltaTracker, CallbackInfo ci) {
        if (!LocomotionMain.CONFIG.data().firstPersonPlayer.enableRenderer) {
            return;
        }
        JointAnimatorDispatcher dispatcher = JointAnimatorDispatcher.getInstance();
        dispatcher.getFirstPersonPlayerDataContainer().ifPresent(dataContainer ->
                JointAnimatorRegistry.getFirstPersonPlayerJointAnimator().ifPresent(jointAnimator ->
                        dispatcher.calculateInterpolatedFirstPersonPlayerPose(dataContainer, deltaTracker.getGameTimeDeltaPartialTick(true))
                )
        );
    }

    @Inject(method = "bobHurt", at = @At("HEAD"))
    private void addCameraRotation(PoseStack poseStack, float partialTicks, CallbackInfo ci) {
        if (!LocomotionMain.CONFIG.data().firstPersonPlayer.enableRenderer) {
            return;
        }
        FirstPersonPlayerRenderer.getInstance().ifPresent(renderer -> renderer.transformCamera(poseStack));
    }

    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void removeViewBobbing(PoseStack poseStack, float partialTicks, CallbackInfo ci) {
        if (LocomotionMain.CONFIG.data().firstPersonPlayer.enableRenderer) {
            ci.cancel();
        }
    }
}
