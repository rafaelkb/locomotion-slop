package com.trainguy9512.locomotion.animation.animator.block_entity;

import com.trainguy9512.locomotion.LocomotionMain;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.LidBlockEntity;

public class ChestJointAnimator<B extends BlockEntity & LidBlockEntity> implements TwoStateContainerJointAnimator<B> {

    public static final ResourceLocation CHEST_SKELETON = LocomotionMain.makeIdentifier("skeletons/block_entity/chest.json");

    public static final ResourceLocation CHEST_OPEN_SEQUENCE = LocomotionMain.makeIdentifier("sequences/block_entity/chest/open.json");
    public static final ResourceLocation CHEST_CLOSE_SEQUENCE = LocomotionMain.makeIdentifier("sequences/block_entity/chest/close.json");

    @Override
    public ResourceLocation getJointSkeleton() {
        return CHEST_SKELETON;
    }

    @Override
    public float getOpenProgress(B blockEntity) {
        return blockEntity.getOpenNess(0);
    }

    @Override
    public ResourceLocation getOpenAnimationSequence() {
        return CHEST_OPEN_SEQUENCE;
    }

    @Override
    public ResourceLocation getCloseAnimationSequence() {
        return CHEST_CLOSE_SEQUENCE;
    }
}
