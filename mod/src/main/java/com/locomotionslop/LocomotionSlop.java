package com.locomotionslop;

import com.locomotionslop.animation.animator.FirstPersonPlayerAnimator;
import com.locomotionslop.compat.EmfIntegration;
import com.locomotionslop.compat.EtfIntegration;
import com.locomotionslop.compat.PlayerModelPackProbe;
import com.locomotionslop.resource.SlopResources;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Locomotion Slop - client entry point.
 *
 * <p>One project, two animation systems, no overlap:</p>
 *
 * <ul>
 *     <li><b>First person</b> is animated by this mod, from Trainguy9512's Locomotion rig and
 *         animation set (vendored under {@code assets/locomotion_slop}). The pose is applied to the
 *         player renderer's own arm and sleeve model parts, so the model actually in use - EMF
 *         geometry, ETF textures, a slim or custom skin - is what gets animated.</li>
 *     <li><b>Third person</b> is never touched. Fresh Animations' player addon keeps running under
 *         EMF/ETF exactly as it does without this mod installed; this mod only asks EMF to stay out
 *         of the first-person hands.</li>
 * </ul>
 */
@Mod(value = LocomotionSlop.MOD_ID, dist = Dist.CLIENT)
public final class LocomotionSlop {

    public static final String MOD_ID = "locomotion_slop";
    public static final Logger LOGGER = LoggerFactory.getLogger("LocomotionSlop");

    public LocomotionSlop(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::onClientSetup);
        modEventBus.addListener(this::onRegisterReloadListeners);

        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(this::onClientTick);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(this::onLoggingOut);

        LOGGER.debug("Locomotion Slop loaded (client only).");
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        SlopConfig.get();
        EmfIntegration.init();
        EtfIntegration.init();
        EmfIntegration.registerAnimationVariables();
        EmfIntegration.setPreventFirstPersonHandAnimating(SlopConfig.get().emfDeferFirstPersonHands
                && SlopConfig.get().firstPersonAnimations);
    }

    private void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        // Also runs once at startup, which is what loads the vendored rig and animations.
        event.registerReloadListener(new SlopResources());
    }

    private void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return;
        }
        if (SlopConfig.get().firstPersonAnimations) {
            FirstPersonPlayerAnimator.getInstance().tick(player, minecraft);
        }
    }

    private void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        FirstPersonPlayerAnimator.getInstance().reset();
    }

    /**
     * Called after a resource reload so the player-model pack report reflects the newly applied
     * packs.
     */
    public static void reportPlayerModelPacks(net.minecraft.server.packs.resources.ResourceManager resourceManager) {
        if (SlopConfig.get().logCompatDiagnostics) {
            PlayerModelPackProbe.logActivePlayerModelPacks(resourceManager);
        }
    }

    public static void logCompat(String message) {
        if (SlopConfig.get().logCompatDiagnostics) {
            LOGGER.info("[compat] {}", message);
        }
    }
}
