package com.trainguy9512.locomotion.mixin.render;

import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/**
 * 1.21.1 keeps {@code ModelPart.children} private and has no part lookup by name, so the child map is
 * exposed through an accessor. The pose code needs it to find nested model parts such as sleeves.
 */
@Mixin(ModelPart.class)
public interface ModelPartAccessor {

    @Accessor("children")
    Map<String, ModelPart> locomotion$getChildren();
}
