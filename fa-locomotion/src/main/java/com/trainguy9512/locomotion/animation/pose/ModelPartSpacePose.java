package com.trainguy9512.locomotion.animation.pose;

import com.trainguy9512.locomotion.access.MatrixModelPart;
import com.trainguy9512.locomotion.animation.joint.skeleton.JointSkeleton;
import net.minecraft.client.model.geom.ModelPart;

public class ModelPartSpacePose extends Pose {

    protected ModelPartSpacePose(Pose pose) {
        super(pose);
    }

    static ModelPartSpacePose of(Pose pose) {
        return new ModelPartSpacePose(pose);
    }

    /**
     * Applies this pose onto a baked model root.
     * 1.21.1 {@code Model} has no {@code resetPose()} or {@code createPartLookup()}, and this port
     * does not call this for the player. Fresh Animations owns that model.
     */
    public void setupAnimOnModel(ModelPart root) {
        JointSkeleton jointSkeleton = this.getJointSkeleton();
        jointSkeleton.getJoints().forEach(joint -> {
            String modelPartName = jointSkeleton.getJointConfiguration(joint).modelPartResourceLocation();
            if (modelPartName != null) {
                ModelPart modelPart = findPart(root, modelPartName);
                if (modelPart != null) {
                    ((MatrixModelPart) (Object) modelPart).locomotion$setMatrix(this.getJointChannel(joint).getTransform());
                }
            }
        });
    }

    private static ModelPart findPart(ModelPart part, String name) {
        if (part.hasChild(name)) {
            return part.getChild(name);
        }
        for (ModelPart child : part.children.values()) {
            ModelPart found = findPart(child, name);
            if (found != null) {
                return found;
            }
        }
        return null;
    }
}
