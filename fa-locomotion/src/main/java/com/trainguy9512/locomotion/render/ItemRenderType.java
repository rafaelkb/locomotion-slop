package com.trainguy9512.locomotion.render;

import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;

public enum ItemRenderType {
    THIRD_PERSON_ITEM,
    MIRRORED_THIRD_PERSON_ITEM,
    MAP,
    /**
     * 1.21.11 has {@code ItemDisplayContext.ON_SHELF}. 1.21.1 does not, so this uses the fixed transform.
     */
    ON_SHELF;

    public boolean isMirrored() {
        return this == MIRRORED_THIRD_PERSON_ITEM;
    }

    public ItemDisplayContext getItemDisplayContext(HumanoidArm side) {
        if (this == ON_SHELF || this == MAP) {
            return ItemDisplayContext.FIXED;
        }
        return side == HumanoidArm.RIGHT ? ItemDisplayContext.THIRD_PERSON_RIGHT_HAND : ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
    }
}
