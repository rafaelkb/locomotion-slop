package com.locomotionslop.render;

import com.locomotionslop.access.MatrixModelPart;
import com.locomotionslop.animation.animator.FirstPersonJoints;
import com.locomotionslop.animation.animator.FirstPersonPlayerAnimator;
import com.locomotionslop.animation.joint.JointChannel;
import com.locomotionslop.animation.pose.ModelPartSpacePose;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

/**
 * Draws the local player's first-person arms and held items using the animated rig.
 *
 * <p>The arm itself is still drawn by vanilla's {@code PlayerRenderer#renderRightHand} /
 * {@code renderLeftHand}: only the model parts' transforms are replaced. That is what keeps this
 * mod compatible with Entity Model Features and Entity Texture Features - whichever model and skin
 * the player renderer is currently using (a Fresh Animations model, a custom skin, a slim model)
 * is exactly what gets drawn, animated by Trainguy's curves.</p>
 */
public final class FirstPersonArmRenderer {

    private static final FirstPersonArmRenderer INSTANCE = new FirstPersonArmRenderer();

    /**
     * The rig is authored Y-up; model parts are Y-down. This is the same conversion Locomotion
     * applies around its first-person arm rendering.
     */
    private static final float RIG_SPACE_FLIP_DEGREES = 180.0F;

    private FirstPersonArmRenderer() {
    }

    public static FirstPersonArmRenderer getInstance() {
        return INSTANCE;
    }

    public void renderHands(float partialTick, PoseStack poseStack, MultiBufferSource buffer, LocalPlayer player, int packedLight) {
        FirstPersonPlayerAnimator animator = FirstPersonPlayerAnimator.getInstance();
        animator.update(player, partialTick);

        ModelPartSpacePose pose = animator.getPose();
        if (pose == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        EntityRenderer<?> entityRenderer = minecraft.getEntityRenderDispatcher().getRenderer(player);
        if (!(entityRenderer instanceof PlayerRenderer playerRenderer)) {
            return;
        }
        PlayerModel<AbstractClientPlayer> model = playerRenderer.getModel();
        if (model == null) {
            return;
        }

        // Off hand first so the main hand draws on top, matching vanilla's draw order.
        this.renderArmWithItem(playerRenderer, model, player, InteractionHand.OFF_HAND, pose, poseStack, buffer, packedLight);
        this.renderArmWithItem(playerRenderer, model, player, InteractionHand.MAIN_HAND, pose, poseStack, buffer, packedLight);
    }

    private void renderArmWithItem(
            PlayerRenderer playerRenderer,
            PlayerModel<AbstractClientPlayer> model,
            LocalPlayer player,
            InteractionHand hand,
            ModelPartSpacePose pose,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        HumanoidArm side = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        JointChannel armChannel = pose.getJointChannel(FirstPersonJoints.armJoint(side));
        JointChannel itemChannel = pose.getJointChannel(FirstPersonJoints.itemJoint(side));

        ModelPart armPart = side == HumanoidArm.LEFT ? model.leftArm : model.rightArm;
        ModelPart sleevePart = side == HumanoidArm.LEFT ? model.leftSleeve : model.rightSleeve;
        Matrix4f armMatrix = armChannel.getTransform();

        ((MatrixModelPart) (Object) armPart).slop$setPendingMatrix(armMatrix);
        ((MatrixModelPart) (Object) sleevePart).slop$setPendingMatrix(armMatrix);

        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees(RIG_SPACE_FLIP_DEGREES));
        try {
            if (side == HumanoidArm.RIGHT) {
                playerRenderer.renderRightHand(poseStack, buffer, packedLight, player);
            } else {
                playerRenderer.renderLeftHand(poseStack, buffer, packedLight, player);
            }
        } finally {
            // Never leave a matrix behind: the same ModelPart instances are used for third person,
            // where Fresh Animations / EMF must see vanilla transforms.
            ((MatrixModelPart) (Object) armPart).slop$clearPendingMatrix();
            ((MatrixModelPart) (Object) sleevePart).slop$clearPendingMatrix();
            poseStack.popPose();
        }

        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty()) {
            return;
        }

        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees(RIG_SPACE_FLIP_DEGREES));
        try {
            itemChannel.transformPoseStack(poseStack, 16.0F);
            ItemDisplayContext displayContext = side == HumanoidArm.RIGHT
                    ? ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                    : ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
            Minecraft.getInstance().getItemRenderer().renderStatic(
                    player,
                    stack,
                    displayContext,
                    side == HumanoidArm.LEFT,
                    poseStack,
                    buffer,
                    player.level(),
                    packedLight,
                    OverlayTexture.NO_OVERLAY,
                    player.getId() + displayContext.ordinal()
            );
        } finally {
            poseStack.popPose();
        }
    }
}
