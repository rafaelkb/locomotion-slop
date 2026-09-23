package com.trainguy9512.locomotion.util;

import net.minecraft.world.item.UseAnim;

public class LocomotionMultiVersionWrappers {
    public static UseAnim getTridentUseAnimation() {
        // 1.21.1 still calls the trident use animation SPEAR. TRIDENT was added later.
        return UseAnim.SPEAR;
    }
}
