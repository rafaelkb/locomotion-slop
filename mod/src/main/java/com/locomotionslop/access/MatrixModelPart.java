package com.locomotionslop.access;

import org.joml.Matrix4f;
import org.jetbrains.annotations.Nullable;

/**
 * Duck interface added to {@link net.minecraft.client.model.geom.ModelPart} by
 * {@code MixinModelPart}.
 *
 * <p>The pending matrix is one-shot: it is consumed by the next
 * {@code ModelPart#translateAndRotate(PoseStack)} call. That ordering matters - vanilla resets the
 * part's own x/y/z and rotation fields inside {@code PlayerRenderer#renderHand} <em>after</em> we
 * set the matrix, but {@code translateAndRotate} only runs when the part actually draws, so the
 * animation always wins. Anything left unconsumed is cleared by the renderer's finally block so a
 * matrix can never leak into third-person rendering.</p>
 */
public interface MatrixModelPart {

    void slop$setPendingMatrix(@Nullable Matrix4f matrix);

    @Nullable
    Matrix4f slop$getPendingMatrix();

    void slop$clearPendingMatrix();
}
