package com.locomotionslop.mixin;

import com.locomotionslop.access.MatrixModelPart;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelPart.class)
public abstract class MixinModelPart implements MatrixModelPart {

    @Unique
    private Matrix4f slop$pendingMatrix;

    @Unique
    @Override
    public void slop$setPendingMatrix(@Nullable Matrix4f matrix) {
        this.slop$pendingMatrix = matrix;
    }

    @Unique
    @Override
    @Nullable
    public Matrix4f slop$getPendingMatrix() {
        return this.slop$pendingMatrix;
    }

    @Unique
    @Override
    public void slop$clearPendingMatrix() {
        this.slop$pendingMatrix = null;
    }

    /**
     * Replaces the part's own translate/rotate with the animation matrix. Joint translations are
     * stored in model-part pixels, so they are divided by 16 to reach block space.
     */
    @Inject(method = "translateAndRotate", at = @At("HEAD"), cancellable = true)
    private void slop$applyPendingMatrix(PoseStack poseStack, CallbackInfo ci) {
        Matrix4f matrix = this.slop$pendingMatrix;
        if (matrix == null) {
            return;
        }
        this.slop$pendingMatrix = null;
        Vector3f translation = matrix.getTranslation(new Vector3f()).div(16.0F);
        poseStack.mulPose(new Matrix4f(matrix).setTranslation(translation));
        ci.cancel();
    }
}
