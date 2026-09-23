package com.locomotionslop.animation.animator;

import com.locomotionslop.animation.joint.JointChannel;
import com.locomotionslop.animation.joint.skeleton.JointSkeleton;
import com.locomotionslop.animation.pose.LocalSpacePose;
import com.locomotionslop.animation.pose.ModelPartSpacePose;
import com.locomotionslop.resource.SlopResources;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.HumanoidArm;
import org.jetbrains.annotations.Nullable;

/**
 * Drives Trainguy's first-person rig from live player state and produces the pose the arm renderer
 * applies.
 *
 * <p>Only the local player's first-person arms are touched. Third-person rendering is left
 * completely alone so that Fresh Animations Player Add-on (through Entity Model Features) keeps
 * full ownership of the player model in third person.</p>
 */
public final class FirstPersonPlayerAnimator {

    private static final FirstPersonPlayerAnimator INSTANCE = new FirstPersonPlayerAnimator();

    private final MovementLayer movement = new MovementLayer();
    private final HandLayer mainHand = new HandLayer(true);
    private final HandLayer offHand = new HandLayer(false);

    private final FirstPersonDrivers previous = new FirstPersonDrivers();
    private final FirstPersonDrivers current = new FirstPersonDrivers();
    private final FirstPersonDrivers frame = new FirstPersonDrivers();

    @Nullable
    private ModelPartSpacePose pose;
    /**
     * Camera joint, taken from the un-mirrored pose. The rig has no mirror partner for the camera,
     * so mirroring it would flip the bob sideways for left-handed players.
     */
    @Nullable
    private JointChannel cameraChannel;
    private boolean initialised;

    private FirstPersonPlayerAnimator() {
    }

    public static FirstPersonPlayerAnimator getInstance() {
        return INSTANCE;
    }

    /**
     * Drops all playback state. Called when the player changes or the renderer is toggled off, so a
     * stale pose can never leak into the next session.
     */
    public void reset() {
        this.movement.reset();
        this.mainHand.reset();
        this.offHand.reset();
        this.previous.copyFrom(this.current);
        this.initialised = false;
        this.pose = null;
        this.cameraChannel = null;
    }

    /**
     * Advances the state machine. Must run once per client tick.
     */
    public void tick(LocalPlayer player, Minecraft minecraft) {
        this.previous.copyFrom(this.current);
        this.current.capture(player, minecraft, this.initialised ? this.previous : null);
        this.initialised = true;

        this.movement.tick(this.current);
        this.mainHand.tick(this.current);
        this.offHand.tick(this.current);
    }

    /**
     * Evaluates the pose for this rendered frame.
     */
    public void update(LocalPlayer player, float partialTick) {
        JointSkeleton skeleton = SlopResources.getJointSkeletonOrNull(FirstPersonJoints.SKELETON);
        if (skeleton == null) {
            this.pose = null;
            this.cameraChannel = null;
            return;
        }

        this.frame.interpolate(this.previous, this.current, partialTick);

        LocalSpacePose pose = this.movement.sample(skeleton, partialTick);

        // The off hand yields while the main hand is doing something that needs both arms.
        if (!this.mainHand.isPlayingTwoHandedMontage()) {
            LocalSpacePose offHandPose = this.offHand.sample(skeleton, partialTick);
            if (offHandPose != null) {
                pose.interpolated(offHandPose, this.offHand.weight(), this.offHand.mask(), pose);
            }
        }

        LocalSpacePose mainHandPose = this.mainHand.sample(skeleton, partialTick);
        if (mainHandPose != null) {
            pose.interpolated(mainHandPose, this.mainHand.weight(), this.mainHand.mask(), pose);
        }

        ModelPartSpacePose modelPartSpacePose = pose.convertedToComponentSpace().convertedToModelPartSpace();
        this.cameraChannel = modelPartSpacePose.getJointChannel(FirstPersonJoints.CAMERA);

        // The rig is authored with the right arm as the main hand; lefties get the mirrored pose.
        if (player.getMainArm() == HumanoidArm.LEFT) {
            modelPartSpacePose = ModelPartSpacePose.ofMirrored(modelPartSpacePose);
        }

        this.pose = modelPartSpacePose;
    }

    @Nullable
    public ModelPartSpacePose getPose() {
        return this.pose;
    }

    /**
     * The camera joint transform for this frame, in model-part space (translations in pixels).
     */
    @Nullable
    public JointChannel getCameraChannel() {
        return this.cameraChannel;
    }


    public boolean hasPose() {
        return this.pose != null;
    }

    public boolean isMining() {
        return this.currentDrivers != null && this.currentDrivers.mining;
    }

    public boolean isAttacking() {
        return this.mainHand.isMontageActive() || this.offHand.isMontageActive();
    }

    public boolean isUsingItem() {
        return this.currentDrivers != null && this.currentDrivers.usingItem;
    }

    public boolean isSprinting() {
        return this.movementLayer.isSprinting();
    }

    public boolean isSneaking() {
        return this.currentDrivers != null && this.currentDrivers.sneaking;
    }

    public boolean isFalling() {
        return this.currentDrivers != null && this.currentDrivers.falling;
    }

    public boolean isSwimming() {
        return this.currentDrivers != null && this.currentDrivers.swimming;
    }

    public String debugState() {
        return "movement=" + this.movement.debugState()
                + " mainHand=" + this.mainHand.kind()
                + " offHand=" + this.offHand.kind();
    }
}
