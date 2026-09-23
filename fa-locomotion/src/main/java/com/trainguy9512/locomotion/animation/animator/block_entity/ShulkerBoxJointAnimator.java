package com.trainguy9512.locomotion.animation.animator.block_entity;

import com.trainguy9512.locomotion.LocomotionMain;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import org.jetbrains.annotations.NotNull;

public class ShulkerBoxJointAnimator implements TwoStateContainerJointAnimator<@org.jetbrains.annotations.NotNull ShulkerBoxBlockEntity> {

    public static final ResourceLocation SHULKER_BOX_SKELETON = LocomotionMain.makeIdentifier("skeletons/block_entity/shulker_box.json");

    public static final ResourceLocation SHULKER_BOX_OPEN_SEQUENCE = LocomotionMain.makeIdentifier("sequences/block_entity/shulker_box/open.json");
    public static final ResourceLocation SHULKER_BOX_CLOSE_SEQUENCE = LocomotionMain.makeIdentifier("sequences/block_entity/shulker_box/close.json");

    @Override
    public ResourceLocation getJointSkeleton() {
        return SHULKER_BOX_SKELETON;
    }

    @Override
    public float getOpenProgress(@NotNull ShulkerBoxBlockEntity blockEntity) {
        return blockEntity.getProgress(0);
    }

    @Override
    public ResourceLocation getOpenAnimationSequence() {
        return SHULKER_BOX_OPEN_SEQUENCE;
    }

    @Override
    public ResourceLocation getCloseAnimationSequence() {
        return SHULKER_BOX_CLOSE_SEQUENCE;
    }
}
