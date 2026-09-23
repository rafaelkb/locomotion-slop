package com.trainguy9512.locomotion.compat;

import com.trainguy9512.locomotion.LocomotionMain;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Entity Model Features is not a compile dependency. If it is installed, force the option that
 * stops EMF from posing the first-person arm. Locomotion draws that arm itself. Third person stays with EMF.
 */
public final class EmfCompat {
    private static boolean applied;

    private EmfCompat() {
    }

    public static void disableFirstPersonHandAnimating() {
        if (applied) {
            return;
        }
        try {
            Class<?> emf = Class.forName("traben.entity_model_features.EMF");
            Method config = emf.getMethod("config");
            Object handler = config.invoke(null);
            Method getConfig = handler.getClass().getMethod("getConfig");
            Object emfConfig = getConfig.invoke(handler);
            Field field = emfConfig.getClass().getField("preventFirstPersonHandAnimating");
            field.setBoolean(emfConfig, true);
            applied = true;
            LocomotionMain.DEBUG_LOGGER.info("Told Entity Model Features to leave first-person hands alone.");
        } catch (ClassNotFoundException ignored) {
            applied = true;
        } catch (ReflectiveOperationException exception) {
            LocomotionMain.DEBUG_LOGGER.warn("Could not set EMF preventFirstPersonHandAnimating. Enable it in the EMF config if first-person hands look wrong.", exception);
            applied = true;
        }
    }
}
