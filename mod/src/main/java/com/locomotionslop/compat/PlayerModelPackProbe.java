package com.locomotionslop.compat;

import com.locomotionslop.LocomotionSlop;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.List;

/**
 * Reports whether a player-model pack is actually active, so a user can tell from the log whether
 * their Fresh Animations player addon is loaded and which pack is winning if several are stacked.
 *
 * <p>Detection is passive: it looks for the EMF custom-entity-model files a player pack ships and
 * lists the pack ids that provide them. No assets are read, copied or required - the
 * Fresh Animations player addon is an early-access, non-redistributable pack, so this project
 * integrates with it at runtime instead of shipping it.</p>
 */
public final class PlayerModelPackProbe {

    /** Files a player model pack ships when it targets EMF. */
    private static final ResourceLocation[] PLAYER_MODEL_FILES = {
            ResourceLocation.withDefaultNamespace("emf/cem/player.jem"),
            ResourceLocation.withDefaultNamespace("emf/cem/player_slim.jem"),
            ResourceLocation.withDefaultNamespace("emf/cem/player.properties"),
            // Older / other loaders: OptiFine's own CEM path and FAPA's vanilla-model variant.
            ResourceLocation.withDefaultNamespace("optifine/cem/player.jem"),
            ResourceLocation.withDefaultNamespace("models/entity/player.jem"),
    };

    private PlayerModelPackProbe() {
    }

    public static void logActivePlayerModelPacks(ResourceManager resourceManager) {
        for (ResourceLocation file : PLAYER_MODEL_FILES) {
            List<Resource> resources = resourceManager.getResourceStack(file);
            if (resources.isEmpty()) {
                continue;
            }
            StringBuilder packs = new StringBuilder();
            for (Resource resource : resources) {
                if (packs.length() > 0) {
                    packs.append(", ");
                }
                packs.append(resource.sourcePackId());
            }
            // The last entry in the stack is the one EMF actually loads.
            LocomotionSlop.logCompat("Player model file " + file + " provided by: " + packs
                    + " (last one wins)");
        }
        LocomotionSlop.logCompat("Third-person player animation is left entirely to EMF/ETF and your model pack.");
    }
}
