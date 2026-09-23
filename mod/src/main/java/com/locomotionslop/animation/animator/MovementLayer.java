package com.locomotionslop.animation.animator;

import com.locomotionslop.animation.joint.skeleton.JointSkeleton;
import com.locomotionslop.animation.pose.LocalSpacePose;
import com.locomotionslop.animation.util.Easing;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * The locomotion half of the first-person animation graph: a small state machine over Trainguy's
 * {@code ground_movement} sequences.
 *
 * <p>Walk and sprint cycles are driven by distance travelled rather than wall-clock time, so the
 * arms stay in step with the player's feet. Everything else plays at its authored rate.</p>
 */
public final class MovementLayer {

    /** Blocks of travel covered by one full walk/sprint cycle, matching vanilla's view-bob period. */
    private static final float BLOCKS_PER_CYCLE = 2.0F;

    private static final float WALK_SPEED_THRESHOLD = 0.03F;
    private static final float SPRINT_SPEED_THRESHOLD = 0.18F;
    private static final float TICK_SECONDS = 1.0F / 20.0F;

    private enum State {
        IDLE("ground_movement/idle", true, false),
        WALKING("ground_movement/walking", true, false),
        SPRINTING("ground_movement/sprinting", true, false),
        WALK_TO_STOP("ground_movement/walk_to_stop", false, false),
        RUN_TO_STOP("ground_movement/run_to_stop", false, false),
        CROUCH_IN("ground_movement/crouch_in", false, true),
        CROUCH_OUT("ground_movement/crouch_out", false, false),
        JUMP("ground_movement/jump", false, false),
        JUMP_RUNNING("ground_movement/jump_running", false, false),
        FALLING_UP("ground_movement/falling_up", false, true),
        FALLING_LOOP("ground_movement/falling_loop", true, false),
        FALLING_DOWN("ground_movement/falling_down", false, true),
        FALLING_IN_PLACE("ground_movement/falling_in_place", true, false),
        LAND("ground_movement/land", false, false),
        SWIMMING("ground_movement/swimming_idle", true, false),
        MOUNTED("ground_movement/mount_enter", false, true);

        final ResourceLocation sequence;
        final boolean looping;
        /** True when the state should hold its last frame until something else takes over. */
        final boolean holdsLastFrame;

        State(String path, boolean looping, boolean holdsLastFrame) {
            this.sequence = FirstPersonJoints.sequence(path);
            this.looping = looping;
            this.holdsLastFrame = holdsLastFrame;
        }
    }

    private final SequenceSlot current = new SequenceSlot();
    private final SequenceSlot previous = new SequenceSlot();

    private State state = State.IDLE;
    private boolean transitioning;
    private float transitionTicks;
    private float transitionLength = 4.0F;

    private boolean wasGrounded = true;
    private boolean wasCrouched;

    public MovementLayer() {
        this.enterState(State.IDLE, 0.0F);
    }

    public void reset() {
        this.state = State.IDLE;
        this.transitioning = false;
        this.transitionTicks = 0.0F;
        this.wasGrounded = true;
        this.wasCrouched = false;
        this.previous.clear();
        this.enterState(State.IDLE, 0.0F);
    }

    public String debugState() {
        return this.transitioning ? this.previous.sequence() + " -> " + this.state.name() : this.state.name();
    }

    public void tick(FirstPersonDrivers drivers) {
        boolean justLanded = drivers.grounded && !this.wasGrounded;
        this.wasGrounded = drivers.grounded;

        State desired = this.evaluate(drivers);
        if (justLanded && this.state != State.LAND) {
            desired = State.LAND;
        }

        if (desired != this.state) {
            // Landing always wins, otherwise a looping fall cycle would swallow the land animation.
            boolean canInterrupt = justLanded
                    || this.current.isFinished()
                    || desired == State.WALKING
                    || desired == State.SPRINTING
                    || !drivers.grounded
                    || drivers.riding
                    || drivers.swimming;
            if (canInterrupt) {
                this.beginTransition(desired, transitionTicksFor(desired));
            }
        }

        if (this.transitioning) {
            this.transitionTicks += 1.0F;
            this.previous.advance(TICK_SECONDS);
            if (this.transitionTicks >= this.transitionLength) {
                this.transitioning = false;
                this.previous.clear();
            }
        }

        if (this.state == State.WALKING || this.state == State.SPRINTING) {
            // Distance-driven so the cycle stays locked to the feet.
            float length = this.current.length();
            if (length > 0.0F) {
                this.current.setTime((drivers.walkDistance / BLOCKS_PER_CYCLE) * length);
            }
        } else {
            this.current.advance(TICK_SECONDS);
        }
    }

    /**
     * Samples the layer for this frame, including any cross-fade into the new state.
     */
    public LocalSpacePose sample(JointSkeleton skeleton, float partialTick) {
        LocalSpacePose pose = this.current.sample(skeleton, partialTick * TICK_SECONDS);
        if (pose == null) {
            return LocalSpacePose.of(skeleton);
        }
        if (this.transitioning && this.transitionLength > 0.0F) {
            LocalSpacePose from = this.previous.sample(skeleton, partialTick * TICK_SECONDS);
            if (from != null) {
                float time = Mth.clamp((this.transitionTicks + partialTick) / this.transitionLength, 0.0F, 1.0F);
                return from.interpolatedByTransition(pose, time, Easing.SINE_IN_OUT, null, from);
            }
        }
        return pose;
    }

    private State evaluate(FirstPersonDrivers drivers) {
        if (drivers.riding) {
            return State.MOUNTED;
        }
        if (drivers.swimming) {
            return State.SWIMMING;
        }
        if (!drivers.grounded) {
            if (drivers.verticalVelocity > 0.2F && drivers.airTicks < 6.0F) {
                return drivers.horizontalSpeed > SPRINT_SPEED_THRESHOLD * 0.8F ? State.JUMP_RUNNING : State.JUMP;
            }
            if (drivers.verticalVelocity > 0.02F) {
                return State.FALLING_UP;
            }
            if (drivers.verticalVelocity < -0.55F) {
                return State.FALLING_DOWN;
            }
            return drivers.horizontalSpeed > WALK_SPEED_THRESHOLD ? State.FALLING_LOOP : State.FALLING_IN_PLACE;
        }

        if (drivers.sneaking) {
            this.wasCrouched = true;
            return State.CROUCH_IN;
        }
        if (this.wasCrouched) {
            this.wasCrouched = false;
            return State.CROUCH_OUT;
        }
        if (drivers.sprinting && drivers.horizontalSpeed > SPRINT_SPEED_THRESHOLD * 0.6F) {
            return State.SPRINTING;
        }
        if (drivers.horizontalSpeed > WALK_SPEED_THRESHOLD) {
            return State.WALKING;
        }
        // Stopped: play the matching stop animation once, then settle into idle.
        if (this.state == State.SPRINTING) {
            return State.RUN_TO_STOP;
        }
        if (this.state == State.WALKING || this.state == State.LAND) {
            return State.WALK_TO_STOP;
        }
        if (this.state == State.WALK_TO_STOP || this.state == State.RUN_TO_STOP) {
            return this.current.isFinished() ? State.IDLE : this.state;
        }
        return State.IDLE;
    }

    private static float transitionTicksFor(State state) {
        return switch (state) {
            case LAND -> 3.0F;
            case JUMP, JUMP_RUNNING -> 2.0F;
            case FALLING_UP, FALLING_LOOP, FALLING_DOWN, FALLING_IN_PLACE -> 3.0F;
            case CROUCH_IN, CROUCH_OUT -> 6.0F;
            case MOUNTED, SWIMMING -> 5.0F;
            default -> 4.0F;
        };
    }

    private void beginTransition(State next, float transitionTicks) {
        if (transitionTicks <= 0.0F) {
            this.enterState(next, 0.0F);
            return;
        }
        this.previous.clear();
        if (this.current.sequence() != null) {
            this.previous.start(this.current.sequence(), this.state.looping, 1.0F);
            this.previous.setTime(this.current.time());
        }
        this.transitionLength = transitionTicks;
        this.transitionTicks = 0.0F;
        this.transitioning = true;
        this.enterState(next, transitionTicks);
    }

    private void enterState(State next, float transitionTicks) {
        this.state = next;
        this.current.start(next.sequence, next.looping, 1.0F);
        if (transitionTicks <= 0.0F) {
            this.transitioning = false;
        }
    }
}
