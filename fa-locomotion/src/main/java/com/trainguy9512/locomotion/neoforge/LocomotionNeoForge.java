package com.trainguy9512.locomotion.neoforge;

import com.trainguy9512.locomotion.LocomotionMain;
import com.trainguy9512.locomotion.compat.EmfCompat;
import com.trainguy9512.locomotion.render.FirstPersonPlayerRenderer;
import com.trainguy9512.locomotion.resource.LocomotionResources;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

@Mod(value = LocomotionMain.MOD_ID, dist = Dist.CLIENT)
public class LocomotionNeoForge {
    public LocomotionNeoForge(ModContainer modContainer, IEventBus modEventBus) {
        modEventBus.addListener(this::onClientInitialize);
        modEventBus.addListener(this::onResourceReload);
        modEventBus.addListener(this::onAddPackFinders);
    }

    public void onClientInitialize(FMLClientSetupEvent event) {
        LocomotionMain.initialize();
        event.enqueueWork(() -> {
            FirstPersonPlayerRenderer.create();
            EmfCompat.disableFirstPersonHandAnimating();
        });
    }

    public void onResourceReload(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new LocomotionResources());
    }

    /**
     * Fresh Animations is shipped as a forced-on resource pack so Entity Model Features can see
     * {@code player.jem}. Locomotion does not register a third-person player animator.
     */
    public void onAddPackFinders(AddPackFindersEvent event) {
        event.addPackFinders(
                ResourceLocation.fromNamespaceAndPath(LocomotionMain.MOD_ID, "resourcepacks/fa_player"),
                PackType.CLIENT_RESOURCES,
                Component.literal("Fresh Animations Player"),
                PackSource.BUILT_IN,
                true,
                Pack.Position.TOP
        );
    }
}
