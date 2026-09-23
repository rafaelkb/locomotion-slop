package com.trainguy9512.locomotion.animation.animator.entity;

import com.trainguy9512.locomotion.animation.animator.JointAnimator;
import net.minecraft.client.model.EntityModel;
import net.minecraft.world.entity.Entity;

public interface EntityJointAnimator<T extends Entity> extends JointAnimator<T> {

    /**
     * Final per-frame model tweak. 1.21.1 has no entity render state, so this only receives the model.
     * Third-person player posing is intentionally not applied; Fresh Animations owns that model.
     */
    void postProcessModelParts(EntityModel<?> entityModel);
}
