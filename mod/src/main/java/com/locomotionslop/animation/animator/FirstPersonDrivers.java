package com.locomotionslop.animation.animator;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A snapshot of everything the first-person animation graph reads from the local player.
 *
 * <p>Numeric fields are interpolated between the previous and current snapshot so the pose can be
 * evaluated per rendered frame while the state machine still only makes decisions once per tick.</p>
 */
public final class FirstPersonDrivers {

    /** Horizontal speed in blocks per tick. */
    public float horizontalSpeed;
    /** Vertical speed in blocks per tick; negative is falling. */
    public float verticalVelocity;

    public boolean grounded;
    public boolean sprinting;
    public boolean sneaking;
    public boolean swimming;
    public boolean crawling;
    public boolean riding;

    /** Ticks spent airborne. */
    public float airTicks;
    /** Downward speed at the moment the player left the ground. */
    public float fallStartSpeed;
    /** Cumulative horizontal distance travelled, in blocks. Used to keep walk cycles in step. */
    public float walkDistance;

    /** 0..1 progress of the current arm swing, from {@link LivingEntity#getAttackAnim(float)}. */
    public float attackProgress;
    /** True while the swing is running. */
    public boolean swinging;
    /** Set for one tick when a new swing starts. */
    public boolean attackTriggered;

    /** True while the player appears to be digging a block. */
    public boolean mining;
    /** Set for one tick when digging starts. */
    public boolean miningStarted;

    public boolean usingItem;
    public int useTicks;
    public UseAnim useAnimation = UseAnim.NONE;
    public ItemStack useStack = ItemStack.EMPTY;

    public ItemStack mainHandStack = ItemStack.EMPTY;
    public ItemStack offHandStack = ItemStack.EMPTY;

    public void capture(LocalPlayer player, Minecraft minecraft, FirstPersonDrivers previous) {
        Vec3 delta = player.getDeltaMovement();
        this.horizontalSpeed = Mth.sqrt((float) (delta.x * delta.x + delta.z * delta.z));
        this.verticalVelocity = (float) delta.y;

        this.grounded = player.onGround();
        this.sprinting = player.isSprinting();
        this.sneaking = player.isShiftKeyDown() && !player.getAbilities().flying;
        this.swimming = player.isSwimming() || player.isVisuallySwimming();
        this.crawling = player.getPose() == net.minecraft.world.entity.Pose.CRAWLING;
        this.riding = player.isPassenger();

        if (this.grounded) {
            this.airTicks = 0.0F;
            this.walkDistance = (previous == null ? 0.0F : previous.walkDistance) + this.horizontalSpeed;
        } else {
            this.airTicks = (previous == null ? 0.0F : previous.airTicks) + 1.0F;
            this.walkDistance = previous == null ? 0.0F : previous.walkDistance;
            if (this.airTicks <= 1.0F) {
                this.fallStartSpeed = this.verticalVelocity;
            } else {
                this.fallStartSpeed = previous == null ? 0.0F : previous.fallStartSpeed;
            }
        }

        this.attackProgress = player.getAttackAnim(1.0F);
        this.swinging = this.attackProgress > 0.0F;

        // Mining is approximated from the attack key plus what the crosshair is on: holding attack
        // while aimed at a block is digging, anything else that swings is an attack. The exact
        // signal would be MultiPlayerGameMode#startDestroyBlock/continueDestroyBlock; reading the
        // key state keeps this mod to three mixins and still lines up with the dig animation.
        this.mining = minecraft.options.keyAttack.isDown() && minecraft.hitResult instanceof BlockHitResult;
        this.miningStarted = this.mining && (previous == null || !previous.mining);
        this.attackTriggered = this.swinging && !this.mining && (previous == null || !previous.swinging);

        this.usingItem = player.isUsingItem();
        this.useTicks = player.getTicksUsingItem();
        this.useStack = this.usingItem ? player.getUseItem() : ItemStack.EMPTY;
        this.useAnimation = this.usingItem ? this.useStack.getUseAnimation() : UseAnim.NONE;

        this.mainHandStack = player.getMainHandItem();
        this.offHandStack = player.getOffhandItem();
    }

    public void copyFrom(FirstPersonDrivers other) {
        this.horizontalSpeed = other.horizontalSpeed;
        this.verticalVelocity = other.verticalVelocity;
        this.grounded = other.grounded;
        this.sprinting = other.sprinting;
        this.sneaking = other.sneaking;
        this.swimming = other.swimming;
        this.crawling = other.crawling;
        this.riding = other.riding;
        this.airTicks = other.airTicks;
        this.fallStartSpeed = other.fallStartSpeed;
        this.walkDistance = other.walkDistance;
        this.attackProgress = other.attackProgress;
        this.swinging = other.swinging;
        this.attackTriggered = other.attackTriggered;
        this.mining = other.mining;
        this.miningStarted = other.miningStarted;
        this.usingItem = other.usingItem;
        this.useTicks = other.useTicks;
        this.useAnimation = other.useAnimation;
        this.useStack = other.useStack;
        this.mainHandStack = other.mainHandStack;
        this.offHandStack = other.offHandStack;
    }

    /**
     * Writes a per-frame interpolation of {@code previous} and {@code current} into this instance.
     */
    public void interpolate(FirstPersonDrivers previous, FirstPersonDrivers current, float partialTick) {
        float weight = Mth.clamp(partialTick, 0.0F, 1.0F);
        this.horizontalSpeed = Mth.lerp(weight, previous.horizontalSpeed, current.horizontalSpeed);
        this.verticalVelocity = Mth.lerp(weight, previous.verticalVelocity, current.verticalVelocity);
        this.airTicks = Mth.lerp(weight, previous.airTicks, current.airTicks);
        this.fallStartSpeed = Mth.lerp(weight, previous.fallStartSpeed, current.fallStartSpeed);
        this.walkDistance = Mth.lerp(weight, previous.walkDistance, current.walkDistance);
        this.attackProgress = Mth.lerp(weight, previous.attackProgress, current.attackProgress);

        this.grounded = current.grounded;
        this.sprinting = current.sprinting;
        this.sneaking = current.sneaking;
        this.swimming = current.swimming;
        this.crawling = current.crawling;
        this.riding = current.riding;
        this.swinging = current.swinging;
        this.attackTriggered = false;
        this.mining = current.mining;
        this.miningStarted = false;
        this.usingItem = current.usingItem;
        this.useTicks = current.useTicks;
        this.useAnimation = current.useAnimation;
        this.useStack = current.useStack;
        this.mainHandStack = current.mainHandStack;
        this.offHandStack = current.offHandStack;
    }
}
