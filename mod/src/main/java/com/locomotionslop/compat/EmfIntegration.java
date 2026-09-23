package com.locomotionslop.compat;

import com.locomotionslop.LocomotionSlop;
import com.locomotionslop.SlopConfig;
import com.locomotionslop.animation.animator.FirstPersonPlayerAnimator;
import net.neoforged.fml.ModList;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.BooleanSupplier;

/**
 * Optional bridge to Entity Model Features (EMF, {@code entity_model_features}).
 *
 * <p>EMF replaces the player model with the pack's JEM geometry and animates it with its own
 * animation system. Two things have to happen for the two systems to coexist:</p>
 *
 * <ol>
 *     <li>EMF must not fight over the first-person arms. EMF ships exactly one switch for this -
 *         {@code EMFConfig.preventFirstPersonHandAnimating} - and this mod flips it while its own
 *         first-person layer is on. Everything EMF does in third person, including the Fresh
 *         Animations player addon, is untouched.</li>
 *     <li>Packs should be able to see what we are doing, so a handful of {@code slop_*} animation
 *         variables are published through {@code EMFAnimationApi}. A JPM can then key off
 *         {@code slop_mining} or {@code slop_sprinting} the same way it would key off any other
 *         variable.</li>
 * </ol>
 *
 * <p>All of this is reflective: EMF is an optional runtime dependency, never a compile-time one,
 * and every call is wrapped so that an EMF API change degrades into a log line rather than a crash.
 * Verified against EMF 3.3.x ({@code EMFAnimationApi#getApiVersion()} == 11).</p>
 */
public final class EmfIntegration {

    private static final String MOD_ID = "entity_model_features";
    private static final String CONFIG_CLASS = "traben.entity_model_features.EMF";
    private static final String API_CLASS = "traben.entity_model_features.utils.api.EMFAnimationApi";
    private static final String FIELD_PREVENT_FIRST_PERSON_HAND = "preventFirstPersonHandAnimating";

    private static boolean initialised;
    private static boolean present;
    private static Object emfConfig;
    private static Field preventFirstPersonHandField;

    private EmfIntegration() {
    }

    public static void init() {
        if (initialised) {
            return;
        }
        initialised = true;
        present = ModList.get().isLoaded(MOD_ID);

        if (!present) {
            LocomotionSlop.logCompat("EMF not present; first-person arms will be drawn by vanilla's player model.");
            return;
        }

        try {
            Class<?> apiClass = Class.forName(API_CLASS);
            int apiVersion = (Integer) apiClass.getMethod("getApiVersion").invoke(null);
            LocomotionSlop.logCompat("EMF found, animation API version " + apiVersion + ".");
        } catch (ReflectiveOperationException | RuntimeException exception) {
            LocomotionSlop.logCompat("EMF found but its animation API is not readable (" + exception + ").");
        }

        try {
            Object handler = Class.forName(CONFIG_CLASS).getMethod("config").invoke(null);
            emfConfig = handler.getClass().getMethod("getConfig").invoke(handler);
            preventFirstPersonHandField = emfConfig.getClass().getField(FIELD_PREVENT_FIRST_PERSON_HAND);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            LocomotionSlop.LOGGER.warn("[EMF] Could not reach EMFConfig.{}; first-person hands may be animated twice.",
                    FIELD_PREVENT_FIRST_PERSON_HAND, exception);
        }
    }

    public static boolean isPresent() {
        return present;
    }

    /**
     * Tells EMF whether to skip its own first-person hand animation. Passing {@code false} restores
     * EMF's own value semantics, so a player who disables our first-person layer gets EMF's hands
     * back rather than no hands at all.
     */
    public static void setPreventFirstPersonHandAnimating(boolean prevent) {
        if (preventFirstPersonHandField == null || emfConfig == null) {
            return;
        }
        try {
            preventFirstPersonHandField.setBoolean(emfConfig, prevent);
        } catch (IllegalAccessException exception) {
            LocomotionSlop.LOGGER.warn("[EMF] Could not set {}.", FIELD_PREVENT_FIRST_PERSON_HAND, exception);
        }
    }

    /**
     * Publishes this mod's animation state as EMF animation variables.
     */
    public static void registerAnimationVariables() {
        if (!present || !SlopConfig.get().emfExposeVariables) {
            return;
        }
        try {
            Class<?> apiClass = Class.forName(API_CLASS);
            Method register = apiClass.getMethod("registerSingletonAnimationVariable",
                    String.class, String.class, String.class, BooleanSupplier.class);

            registerVariable(register, "slop_first_person_active", "Is Locomotion Slop driving the first-person arms");
            registerVariable(register, "slop_mining", "Is the player mining a block");
            registerVariable(register, "slop_attacking", "Is an attack animation playing");
            registerVariable(register, "slop_using_item", "Is the player using the held item");
            registerVariable(register, "slop_sprinting", "Is the player sprinting");
            registerVariable(register, "slop_sneaking", "Is the player sneaking");
            registerVariable(register, "slop_falling", "Is the player falling");
            registerVariable(register, "slop_swimming", "Is the player swimming");

            LocomotionSlop.logCompat("Published 8 slop_* animation variables to EMF.");
        } catch (ReflectiveOperationException | RuntimeException exception) {
            LocomotionSlop.LOGGER.warn("[EMF] Could not register animation variables.", exception);
        }
    }

    private static void registerVariable(Method register, String name, String tooltip) throws ReflectiveOperationException {
        register.invoke(null, name, "locomotion_slop", tooltip, (BooleanSupplier) () -> state(name));
    }

    private static boolean state(String name) {
        FirstPersonPlayerAnimator animator = FirstPersonPlayerAnimator.getInstance();
        if (!animator.hasPose()) {
            return false;
        }
        return switch (name) {
            case "slop_first_person_active" -> true;
            case "slop_mining" -> animator.isMining();
            case "slop_attacking" -> animator.isAttacking();
            case "slop_using_item" -> animator.isUsingItem();
            case "slop_sprinting" -> animator.isSprinting();
            case "slop_sneaking" -> animator.isSneaking();
            case "slop_falling" -> animator.isFalling();
            case "slop_swimming" -> animator.isSwimming();
            default -> false;
        };
    }
}
