package com.locomotionslop.animation.animator;

import com.locomotionslop.LocomotionSlop;
import com.locomotionslop.animation.joint.skeleton.JointMask;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;

import java.util.List;
import java.util.Set;

/**
 * Joint names of the vendored first-person rig, plus the blend masks used to layer the hand
 * animations on top of the movement animation.
 */
public final class FirstPersonJoints {

    public static final String ROOT = "root_jnt";
    public static final String CAMERA = "camera_jnt";
    public static final String ARM_BUFFER = "arm_buffer_jnt";
    public static final String RIGHT_ARM_BUFFER = "arm_R_buffer_jnt";
    public static final String RIGHT_ARM = "arm_R_jnt";
    public static final String RIGHT_HAND = "hand_R_jnt";
    public static final String RIGHT_ITEM = "item_R_jnt";
    public static final String LEFT_ARM_BUFFER = "arm_L_buffer_jnt";
    public static final String LEFT_ARM = "arm_L_jnt";
    public static final String LEFT_HAND = "hand_L_jnt";
    public static final String LEFT_ITEM = "item_L_jnt";

    public static final Set<String> RIGHT_SIDE = Set.of(RIGHT_ARM_BUFFER, RIGHT_ARM, RIGHT_HAND, RIGHT_ITEM);
    public static final Set<String> LEFT_SIDE = Set.of(LEFT_ARM_BUFFER, LEFT_ARM, LEFT_HAND, LEFT_ITEM);

    /** Everything the hand layers are allowed to write to. */
    public static final Set<String> BOTH_SIDES = Set.of(
            ARM_BUFFER,
            RIGHT_ARM_BUFFER, RIGHT_ARM, RIGHT_HAND, RIGHT_ITEM,
            LEFT_ARM_BUFFER, LEFT_ARM, LEFT_HAND, LEFT_ITEM
    );

    public static final JointMask RIGHT_MASK = JointMask.builder().includeJoints(RIGHT_SIDE).build();
    public static final JointMask LEFT_MASK = JointMask.builder().includeJoints(LEFT_SIDE).build();
    public static final JointMask BOTH_ARMS_MASK = JointMask.builder().includeJoints(BOTH_SIDES).build();

    /** Path of the vendored first-person skeleton, as indexed by the resource loader. */
    public static final ResourceLocation SKELETON =
            ResourceLocation.fromNamespaceAndPath(LocomotionSlop.MOD_ID, "skeletons/entity/player/first_person.json");

    private static final List<String> RIGHT_SIDE_LIST = List.copyOf(RIGHT_SIDE);
    private static final List<String> LEFT_SIDE_LIST = List.copyOf(LEFT_SIDE);

    private FirstPersonJoints() {
    }

    public static JointMask maskFor(HumanoidArm side) {
        return side == HumanoidArm.LEFT ? LEFT_MASK : RIGHT_MASK;
    }

    public static String armJoint(HumanoidArm side) {
        return side == HumanoidArm.LEFT ? LEFT_ARM : RIGHT_ARM;
    }

    public static String itemJoint(HumanoidArm side) {
        return side == HumanoidArm.LEFT ? LEFT_ITEM : RIGHT_ITEM;
    }

    public static List<String> jointsFor(HumanoidArm side) {
        return side == HumanoidArm.LEFT ? LEFT_SIDE_LIST : RIGHT_SIDE_LIST;
    }

    /**
     * @param path path under {@code sequences/entity/player/first_person/}, without the extension
     */
    public static ResourceLocation sequence(String path) {
        return ResourceLocation.fromNamespaceAndPath(LocomotionSlop.MOD_ID, "sequences/entity/player/first_person/" + path + ".json");
    }
}
