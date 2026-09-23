package com.trainguy9512.locomotion.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.trainguy9512.locomotion.access.MatrixModelPart;
import com.trainguy9512.locomotion.animation.animator.JointAnimatorDispatcher;
import com.trainguy9512.locomotion.animation.animator.JointAnimatorRegistry;
import com.trainguy9512.locomotion.animation.animator.entity.firstperson.FirstPersonDrivers;
import com.trainguy9512.locomotion.animation.animator.entity.firstperson.FirstPersonJointAnimator;
import com.trainguy9512.locomotion.animation.animator.entity.firstperson.handpose.FirstPersonGenericItems;
import com.trainguy9512.locomotion.animation.animator.entity.firstperson.handpose.FirstPersonHandPoses;
import com.trainguy9512.locomotion.animation.data.AnimationDataContainer;
import com.trainguy9512.locomotion.animation.joint.JointChannel;
import com.trainguy9512.locomotion.animation.pose.ModelPartSpacePose;
import com.trainguy9512.locomotion.compat.EmfCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Objects;
import java.util.Optional;

/**
 * Draws Locomotion's first-person arms with the 1.21.1 item and player-model APIs.
 * The model is baked here so Entity Model Features can keep the third-person player model.
 */
public class FirstPersonPlayerRenderer {
    private static FirstPersonPlayerRenderer instance;

    private final Minecraft minecraft;
    private final JointAnimatorDispatcher jointAnimatorDispatcher;
    private final ModelPart wideRoot;
    private final ModelPart slimRoot;

    public static boolean isRenderingLocomotionFirstPerson = false;
    public static boolean shouldFlipItemTransform = false;
    public static InteractionHand currentItemInteractionHand = InteractionHand.MAIN_HAND;
    public static float currentPartialTicks = 0;

    public static void create() {
        instance = new FirstPersonPlayerRenderer();
    }

    public static Optional<FirstPersonPlayerRenderer> getInstance() {
        return Optional.ofNullable(instance);
    }

    private FirstPersonPlayerRenderer() {
        this.minecraft = Minecraft.getInstance();
        this.jointAnimatorDispatcher = JointAnimatorDispatcher.getInstance();
        this.wideRoot = bake(false);
        this.slimRoot = bake(true);
    }

    private static ModelPart bake(boolean slim) {
        MeshDefinition mesh = PlayerModel.createMesh(CubeDeformation.NONE, slim);
        return LayerDefinition.create(mesh, 64, 64).bakeRoot();
    }

    public void renderLocomotionArmWithItem(
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            AbstractClientPlayer player,
            int combinedLight,
            InteractionHand hand
    ) {
        EmfCompat.disableFirstPersonHandAnimating();
        currentPartialTicks = partialTick;
        this.jointAnimatorDispatcher.getFirstPersonPlayerDataContainer().ifPresent(dataContainer ->
                JointAnimatorRegistry.getFirstPersonPlayerJointAnimator().ifPresent(jointAnimator ->
                        this.jointAnimatorDispatcher.calculateInterpolatedFirstPersonPlayerPose(dataContainer, partialTick)
                )
        );
        if (this.jointAnimatorDispatcher.getInterpolatedFirstPersonPlayerPose().isEmpty()) {
            return;
        }

        boolean leftHanded = this.minecraft.options.mainHand().get() == HumanoidArm.LEFT;
        HumanoidArm side = hand == InteractionHand.MAIN_HAND ? HumanoidArm.RIGHT : HumanoidArm.LEFT;
        if (leftHanded) {
            side = side.getOpposite();
        }

        AnimationDataContainer dataContainer = this.jointAnimatorDispatcher.getFirstPersonPlayerDataContainer().get();
        ModelPartSpacePose pose = this.jointAnimatorDispatcher.getInterpolatedFirstPersonPlayerPose().get();
        JointChannel armPose = pose.getJointChannel(FirstPersonJointAnimator.getArmJoint(side));
        JointChannel itemPose = pose.getJointChannel(FirstPersonJointAnimator.getItemJoint(side));

        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees(180));
        this.renderArm(player, side, armPose, poseStack, bufferSource, combinedLight);

        ResourceLocation genericItemPose = dataContainer.getDriverValue(FirstPersonDrivers.getGenericItemPoseDriver(hand));
        FirstPersonGenericItems.GenericItemPoseDefinition genericItemPoseDefinition = FirstPersonGenericItems.getOrThrowFromResourceLocation(genericItemPose);
        ResourceLocation handPoseLocation = dataContainer.getDriverValue(FirstPersonDrivers.getHandPoseDriver(hand));
        FirstPersonHandPoses.HandPoseDefinition handPose = FirstPersonHandPoses.getOrThrowFromResourceLocation(handPoseLocation);
        ItemRenderType itemRenderType = handPoseLocation == FirstPersonHandPoses.GENERIC_ITEM
                ? genericItemPoseDefinition.itemRenderType()
                : handPose.itemRenderType();

        this.renderItem(
                player,
                getItemStackInHandToRender(dataContainer, player, hand),
                poseStack,
                itemPose,
                bufferSource,
                combinedLight,
                side,
                hand,
                itemRenderType
        );
        poseStack.popPose();
    }

    private static ItemStack getItemStackInHandToRender(AnimationDataContainer dataContainer, AbstractClientPlayer player, InteractionHand hand) {
        ItemStack driverRenderedItem = dataContainer.getDriverValue(FirstPersonDrivers.getRenderedItemDriver(hand));
        ItemStack playerItem = player.getItemInHand(hand);
        if (!ItemStack.isSameItem(playerItem, driverRenderedItem)) {
            return driverRenderedItem;
        }
        if (ItemStack.isSameItemSameComponents(playerItem, driverRenderedItem)) {
            return playerItem;
        }
        for (TypedDataComponent<?> dataComponent : playerItem.getComponents()) {
            if (dataComponent.type() == DataComponents.DAMAGE) {
                continue;
            }
            if (!driverRenderedItem.getComponents().has(dataComponent.type())) {
                return driverRenderedItem;
            }
            if (!Objects.equals(driverRenderedItem.get(dataComponent.type()), dataComponent.value())) {
                return driverRenderedItem;
            }
        }
        return playerItem;
    }

    private void renderArm(
            AbstractClientPlayer player,
            HumanoidArm arm,
            JointChannel armPose,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int combinedLight
    ) {
        PlayerSkin skin = player.getSkin();
        boolean slim = skin.model() == PlayerSkin.Model.SLIM;
        boolean leftArm = arm == HumanoidArm.LEFT;
        ResourceLocation texture = skin.texture();
        ModelPart root = slim ? this.slimRoot : this.wideRoot;
        ModelPart armPart = root.getChild(leftArm ? "left_arm" : "right_arm");
        ModelPart sleevePart = root.getChild(leftArm ? "left_sleeve" : "right_sleeve");
        PlayerModelPart sleeveToggle = leftArm ? PlayerModelPart.LEFT_SLEEVE : PlayerModelPart.RIGHT_SLEEVE;

        Matrix4f transform = armPose.getTransform();
        ((MatrixModelPart) (Object) armPart).locomotion$setMatrix(transform);
        ((MatrixModelPart) (Object) sleevePart).locomotion$setMatrix(transform);
        sleevePart.visible = player.isModelPartShown(sleeveToggle);

        poseStack.pushPose();
        if (slim) {
            poseStack.translate(0.5f / 16f * (leftArm ? 1 : -1), 0, 0);
        }
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(texture));
        armPart.render(poseStack, consumer, combinedLight, OverlayTexture.NO_OVERLAY);
        if (sleevePart.visible) {
            sleevePart.render(poseStack, consumer, combinedLight, OverlayTexture.NO_OVERLAY);
        }
        poseStack.popPose();

        ((MatrixModelPart) (Object) armPart).locomotion$setMatrix(null);
        ((MatrixModelPart) (Object) sleevePart).locomotion$setMatrix(null);
    }

    public void renderItem(
            LivingEntity entity,
            ItemStack itemStack,
            PoseStack poseStack,
            JointChannel jointChannel,
            MultiBufferSource bufferSource,
            int combinedLight,
            HumanoidArm side,
            InteractionHand hand,
            ItemRenderType renderType
    ) {
        if (itemStack.isEmpty()) {
            return;
        }
        isRenderingLocomotionFirstPerson = true;
        currentItemInteractionHand = hand;
        poseStack.pushPose();
        jointChannel.transformPoseStack(poseStack, 16f);
        if (renderType.isMirrored() && side == HumanoidArm.LEFT) {
            shouldFlipItemTransform = true;
        }
        switch (renderType) {
            case MAP -> this.renderMap(bufferSource, poseStack, itemStack, combinedLight);
            case THIRD_PERSON_ITEM, MIRRORED_THIRD_PERSON_ITEM, ON_SHELF -> {
                ItemDisplayContext displayContext = renderType.getItemDisplayContext(side);
                this.minecraft.getItemRenderer().renderStatic(
                        entity,
                        itemStack,
                        displayContext,
                        side == HumanoidArm.LEFT,
                        poseStack,
                        bufferSource,
                        entity.level(),
                        combinedLight,
                        OverlayTexture.NO_OVERLAY,
                        entity.getId() + displayContext.ordinal()
                );
            }
        }
        shouldFlipItemTransform = false;
        isRenderingLocomotionFirstPerson = false;
        poseStack.popPose();
    }

    private static final ResourceLocation MAP_BACKGROUND = ResourceLocation.withDefaultNamespace("textures/map/map_background.png");
    private static final ResourceLocation MAP_BACKGROUND_CHECKERBOARD = ResourceLocation.withDefaultNamespace("textures/map/map_background_checkerboard.png");

    private void renderMap(MultiBufferSource bufferSource, PoseStack poseStack, ItemStack itemStack, int combinedLight) {
        MapId mapId = itemStack.get(DataComponents.MAP_ID);
        MapItemSavedData savedData = mapId == null || this.minecraft.level == null ? null : MapItem.getSavedData(mapId, this.minecraft.level);
        ResourceLocation background = savedData == null ? MAP_BACKGROUND : MAP_BACKGROUND_CHECKERBOARD;

        poseStack.scale(-1, 1, -1);
        poseStack.scale(1f / 16f, 1f / 16f, 1f / 16f);
        poseStack.translate(-2, -4, -1);
        poseStack.scale(1f / 8f, 1f / 8f, 1f / 8f);
        poseStack.scale(1 / 4f, 1 / 4f, 1 / 4f);

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.text(background));
        Matrix4f matrix = poseStack.last().pose();
        consumer.addVertex(matrix, -7.0F, 135.0F, 0.0F).setColor(-1).setUv(0.0F, 1.0F).setLight(combinedLight);
        consumer.addVertex(matrix, 135.0F, 135.0F, 0.0F).setColor(-1).setUv(1.0F, 1.0F).setLight(combinedLight);
        consumer.addVertex(matrix, 135.0F, -7.0F, 0.0F).setColor(-1).setUv(1.0F, 0.0F).setLight(combinedLight);
        consumer.addVertex(matrix, -7.0F, -7.0F, 0.0F).setColor(-1).setUv(0.0F, 0.0F).setLight(combinedLight);

        if (savedData != null && mapId != null) {
            this.minecraft.gameRenderer.getMapRenderer().render(poseStack, bufferSource, mapId, savedData, false, combinedLight);
        }
    }

    public void transformCamera(PoseStack poseStack) {
        if (!this.minecraft.options.getCameraType().isFirstPerson()) {
            return;
        }
        this.jointAnimatorDispatcher.getInterpolatedFirstPersonPlayerPose().ifPresent(animationPose -> {
            JointChannel cameraPose = animationPose.getJointChannel(FirstPersonJointAnimator.CAMERA_JOINT);
            Vector3f cameraRot = cameraPose.getEulerRotationZYX();
            cameraRot.z *= -1;
            cameraPose.rotate(cameraRot, JointChannel.TransformSpace.LOCAL, JointChannel.TransformType.REPLACE);
            cameraPose.translate(cameraPose.getTranslation().mul(1, 1, -1), JointChannel.TransformSpace.COMPONENT, JointChannel.TransformType.REPLACE);
            cameraPose.transformPoseStack(poseStack, 16f);
        });
    }
}
