package com.locomotionslop.mixin;

import com.locomotionslop.SlopConfig;
import com.locomotionslop.animation.animator.FirstPersonPlayerAnimator;
import com.locomotionslop.render.FirstPersonArmRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces the hand rendering pass.
 *
 * <p>Both arms and their items are drawn at the head of {@code renderHandsWithItems} (which is the
 * only place the correct {@code partialTick} and light are available together), and the per-hand
 * vanilla pass is cancelled. If the animation data ever fails to load, nothing is cancelled and
 * vanilla hands render as normal.</p>
 */
@Mixin(ItemInHandRenderer.class)
public class MixinItemInHandRenderer {

    @Inject(method = "renderHandsWithItems", at = @At("HEAD"))
    private void slop$renderAnimatedHands(
            float partialTick,
            com.mojang.blaze3d.vertex.PoseStack poseStack,
            MultiBufferSource.BufferSource buffer,
            LocalPlayer player,
            int packedLight,
            CallbackInfo ci
    ) {
        if (!SlopConfig.get().firstPersonAnimations) {
            return;
        }
        FirstPersonArmRenderer.getInstance().renderHands(partialTick, poseStack, buffer, player, packedLight);
    }

    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void slop$skipVanillaArm(
            AbstractClientPlayer player,
            float partialTick,
            float pitch,
            InteractionHand hand,
            float swingProgress,
            ItemStack stack,
            float equippedProgress,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            CallbackInfo ci
    ) {
        if (SlopConfig.get().firstPersonAnimations && FirstPersonPlayerAnimator.getInstance().hasPose()) {
            ci.cancel();
        }
    }
}
