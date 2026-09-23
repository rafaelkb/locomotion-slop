package com.locomotionslop.compat;

import com.locomotionslop.LocomotionSlop;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;

/**
 * Optional bridge to Entity Texture Features (ETF, {@code entity_texture_features}).
 *
 * <p>This mod never touches textures: the arms it draws are drawn by vanilla's player renderer,
 * which already goes through ETF's texture-variant pipeline (custom skins, emissive maps, random
 * variants, the Fresh Animations cape/overlay textures). So the integration is a detection and
 * version report only - the point is to make the "who draws what" story verifiable in the log
 * instead of assumed.</p>
 */
public final class EtfIntegration {

    private static final String MOD_ID = "entity_texture_features";
    private static final String API_CLASS = "traben.entity_texture_features.ETFApi";

    private static boolean present;

    private EtfIntegration() {
    }

    public static void init() {
        present = ModList.get().isLoaded(MOD_ID);
        if (!present) {
            LocomotionSlop.logCompat("ETF not present; skins are drawn from the vanilla texture pipeline.");
            return;
        }
        try {
            Method getApiVersion = Class.forName(API_CLASS).getMethod("getApiVersion");
            LocomotionSlop.logCompat("ETF found, API version " + getApiVersion.invoke(null)
                    + ". Skins, emissive maps and texture variants stay under ETF's control.");
        } catch (ReflectiveOperationException | RuntimeException exception) {
            LocomotionSlop.logCompat("ETF found, but its API is not readable (" + exception + ").");
        }
    }

    public static boolean isPresent() {
        return present;
    }
}
