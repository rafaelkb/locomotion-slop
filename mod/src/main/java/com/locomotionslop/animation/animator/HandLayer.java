package com.locomotionslop.animation.animator;

import com.locomotionslop.animation.joint.skeleton.JointMask;
import com.locomotionslop.animation.joint.skeleton.JointSkeleton;
import com.locomotionslop.animation.pose.LocalSpacePose;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import org.jetbrains.annotations.Nullable;

/**
 * One hand's animation layer: the pose the held item implies, plus a montage on top of it for
 * attacks, digging and item use.
 *
 * <p>Layers are authored in rig space, where the right arm is always the main hand; the
 * orchestrator mirrors the finished pose for left-handed players.</p>
 */
public final class HandLayer {

    private static final float TICK_SECONDS = 1.0F / 20.0F;
    private static final float MONTAGE_FADE_OUT_TICKS = 3.0F;

    private final boolean mainHand;
    /** Rig-space arm side this layer writes to. */
    private final HumanoidArm rigSide;

    private final SequenceSlot poseSlot = new SequenceSlot();
    private final SequenceSlot montageSlot = new SequenceSlot();

    private ItemKind kind = ItemKind.EMPTY;
    private float montageWeight;
    private boolean montageFading;
    private boolean montageTwoHanded;
    private boolean wasMining;

    public HandLayer(boolean mainHand) {
        this.mainHand = mainHand;
        this.rigSide = mainHand ? HumanoidArm.RIGHT : HumanoidArm.LEFT;
        this.setKind(ItemKind.EMPTY, false);
    }

    public void reset() {
        this.wasMining = false;
        this.montageWeight = 0.0F;
        this.montageFading = false;
        this.montageTwoHanded = false;
        this.montageSlot.clear();
        this.setKind(ItemKind.EMPTY, false);
    }

    public HumanoidArm rigSide() {
        return this.rigSide;
    }

    public ItemKind kind() {
        return this.kind;
    }

    /**
     * The mask this layer is allowed to write to. Two-handed actions (drawing a bow, reloading a
     * crossbow, blocking, eating, maps, spyglass) take both arms.
     */
    public JointMask mask() {
        return this.montageTwoHanded && this.montageWeight > 0.0F
                ? FirstPersonJoints.BOTH_ARMS_MASK
                : FirstPersonJoints.maskFor(this.side);
    }

    /**
     * True while any montage - item switch, attack, mining or item use - is still running.
     */
    public boolean isMontageActive() {
        return this.montageSlot.sequence() != null && !this.montageSlot.isFinished();
    }

    public boolean isPlayingTwoHandedMontage() {
        return this.montageTwoHanded && this.montageWeight > 0.5F;
    }

    public void tick(FirstPersonDrivers drivers) {
        ItemStack stack = this.mainHand ? drivers.mainHandStack : drivers.offHandStack;
        ItemKind newKind = ItemKind.of(stack);
        if (newKind != this.kind) {
            this.setKind(newKind, true);
        }

        if (this.mainHand) {
            if (drivers.attackTriggered && this.kind.attack != null) {
                this.startMontage(this.kind.attack, false, false, 1.0F);
            }

            if (drivers.mining && this.kind.mine != null) {
                if (!this.wasMining || !this.isPlaying(this.kind.mine)) {
                    this.startMontage(this.kind.mine, true, false, 1.0F);
                }
            } else if (this.wasMining && this.kind.mineFinish != null) {
                this.startMontage(this.kind.mineFinish, false, false, 1.0F);
            }
            this.wasMining = drivers.mining;

            this.tickItemUse(drivers);
        }

        this.montageSlot.advance(TICK_SECONDS);
        this.poseSlot.advance(TICK_SECONDS);

        if (this.montageSlot.isFinished()) {
            this.montageFading = true;
        }
        if (this.montageFading) {
            this.montageWeight -= 1.0F / MONTAGE_FADE_OUT_TICKS;
            if (this.montageWeight <= 0.0F) {
                this.montageWeight = 0.0F;
                this.montageFading = false;
                this.montageTwoHanded = false;
                this.montageSlot.clear();
            }
        }
    }

    private void tickItemUse(FirstPersonDrivers drivers) {
        if (!drivers.usingItem) {
            return;
        }
        UseAnim useAnimation = drivers.useAnimation;
        switch (useAnimation) {
            case BOW -> {
                // Scrubbed by the pull amount rather than played on a clock, so the draw holds
                // exactly as long as the string is held back.
                this.startMontage(FirstPersonJoints.sequence("hand/bow/pull"), false, true, 1.0F);
                float pull = Mth.clamp(drivers.useTicks / 20.0F, 0.0F, 1.0F);
                this.montageSlot.setTime(pull * this.montageSlot.length());
                this.montageSlot.setRate(0.0F);
            }
            case CROSSBOW -> {
                if (!this.isPlaying(FirstPersonJoints.sequence("hand/crossbow/reload"))) {
                    this.startMontage(FirstPersonJoints.sequence("hand/crossbow/reload"), false, true, 1.0F);
                }
            }
            case BLOCK -> {
                if (!this.isPlaying(FirstPersonJoints.sequence("hand/shield/block_in"))) {
                    this.startMontage(FirstPersonJoints.sequence("hand/shield/block_in"), false, false, 1.0F);
                }
            }
            case SPYGLASS -> {
                // The spyglass pose already holds both arms up; nothing extra to play.
            }
            case EAT -> this.startMontage(FirstPersonJoints.sequence("hand/generic_item/eat_loop"), true, false, 1.0F);
            case DRINK -> this.startMontage(FirstPersonJoints.sequence("hand/generic_item/drink_loop"), true, false, 1.0F);
            case BRUSH -> this.startMontage(FirstPersonJoints.sequence("hand/brush/sift_loop"), true, false, 1.0F);
            case TOOT_HORN -> this.startMontage(FirstPersonJoints.sequence("hand/generic_item/use"), false, false, 1.0F);
            case NONE -> {
            }
            default -> {
                if (!this.isPlaying(FirstPersonJoints.sequence("hand/generic_item/use"))) {
                    this.startMontage(FirstPersonJoints.sequence("hand/generic_item/use"), false, false, 1.0F);
                }
            }
        }
    }

    /**
     * Samples the layer for this frame.
     *
     * @return the layered pose, or null when nothing is loaded
     */
    @Nullable
    public LocalSpacePose sample(JointSkeleton skeleton, float partialTick) {
        LocalSpacePose pose = this.poseSlot.sample(skeleton, partialTick * TICK_SECONDS);
        if (pose == null) {
            return null;
        }
        if (this.montageWeight > 0.0F) {
            LocalSpacePose montage = this.montageSlot.sample(skeleton, partialTick * TICK_SECONDS);
            if (montage != null) {
                pose.interpolated(montage, Mth.clamp(this.montageWeight, 0.0F, 1.0F), null, pose);
            }
        }
        return pose;
    }

    public float weight() {
        return 1.0F;
    }

    private boolean isPlaying(ResourceLocation sequence) {
        return sequence.equals(this.montageSlot.sequence());
    }

    private void startMontage(ResourceLocation sequence, boolean looping, boolean twoHanded, float weight) {
        if (this.isPlaying(sequence)) {
            this.montageTwoHanded = twoHanded;
            return;
        }
        this.montageSlot.start(sequence, looping, 1.0F);
        this.montageTwoHanded = twoHanded;
        this.montageWeight = weight;
        this.montageFading = false;
    }

    private void setKind(ItemKind kind, boolean playRaise) {
        this.kind = kind;
        this.poseSlot.start(kind.pose, false, 1.0F);
        if (playRaise && kind.raise != null) {
            this.startMontage(kind.raise, false, false, 1.0F);
        }
    }
}
