package com.locomotionslop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A deliberately tiny JSON config: {@code config/locomotion_slop.json}.
 *
 * <p>Everything here is a toggle for opting out of a layer, so that a pack author or player can
 * fall back to vanilla behaviour per-system instead of uninstalling the mod.</p>
 */
public final class SlopConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Logger LOGGER = LoggerFactory.getLogger("LocomotionSlop/Config");

    private static SlopConfig instance;

    /** Master switch for the whole first-person layer. Off means 100% vanilla hands and view bob. */
    public boolean firstPersonAnimations = true;
    /** Drive the camera bob from the rig's camera joint instead of vanilla's {@code bobView}. */
    public boolean animationViewBob = true;
    /** Ask EMF to leave first-person hands alone while this mod animates them. */
    public boolean emfDeferFirstPersonHands = true;
    /** Publish {@code slop_*} animation variables to EMF so packs can read our state. */
    public boolean emfExposeVariables = true;
    /** Log the EMF / ETF / Fresh Animations detection results at startup. */
    public boolean logCompatDiagnostics = true;

    public static SlopConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    private static Path configFile() {
        return FMLPaths.CONFIGDIR.get().resolve("locomotion_slop.json");
    }

    private static SlopConfig load() {
        Path file = configFile();
        if (!Files.exists(file)) {
            SlopConfig config = new SlopConfig();
            config.save();
            return config;
        }
        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            SlopConfig config = GSON.fromJson(json, SlopConfig.class);
            return config != null ? config : new SlopConfig();
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("Could not read {}, using defaults", file, exception);
            return new SlopConfig();
        }
    }

    public void save() {
        try {
            Path file = configFile();
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(this), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            LOGGER.warn("Could not write {}", file, exception);
        }
    }
}
