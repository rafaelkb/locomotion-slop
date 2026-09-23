/*
 * Locomotion Slop - SlopResources
 * Resource loading ported from Locomotion (GPLv3).
 */
package com.locomotionslop.resource;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.locomotionslop.animation.joint.skeleton.JointSkeleton;
import com.locomotionslop.animation.sequence.AnimationSequence;
import com.locomotionslop.resource.json.GsonConfiguration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Predicate;

/**
 * Loads joint skeletons from {@code assets/<namespace>/skeletons} and animation sequences from
 * {@code assets/<namespace>/sequences} out of every active resource pack.
 *
 * <p>Because this goes through the normal resource manager, the data shipped inside the mod jar
 * is just the lowest-priority pack: any resource pack placed above it (including a plain
 * {@code resourcepacks} folder) can override or add animations without a rebuild.</p>
 */
public class SlopResources implements PreparableReloadListener {

    private static final Logger LOGGER = LogManager.getLogger("LocomotionSlop/Resources");

    private static final String JOINT_SKELETON_PATH = "skeletons";
    private static final String ANIMATION_SEQUENCE_PATH = "sequences";

    private static final Map<ResourceLocation, AnimationSequence> ANIMATION_SEQUENCES = new HashMap<>();
    private static final Map<ResourceLocation, JointSkeleton> JOINT_SKELETONS = new HashMap<>();

    public static Map<ResourceLocation, JointSkeleton> getJointSkeletons() {
        return JOINT_SKELETONS;
    }

    public static Map<ResourceLocation, AnimationSequence> getAnimationSequences() {
        return ANIMATION_SEQUENCES;
    }

    public static JointSkeleton getOrThrowJointSkeleton(ResourceLocation location) {
        JointSkeleton skeleton = JOINT_SKELETONS.get(location);
        if (skeleton == null) {
            throw new IllegalArgumentException("No joint skeleton loaded for " + location + ". Loaded skeletons: " + JOINT_SKELETONS.keySet());
        }
        return skeleton;
    }

    public static JointSkeleton getJointSkeletonOrNull(ResourceLocation location) {
        return JOINT_SKELETONS.get(location);
    }

    public static AnimationSequence getOrThrowAnimationSequence(ResourceLocation location) {
        AnimationSequence sequence = ANIMATION_SEQUENCES.get(location);
        if (sequence == null) {
            throw new IllegalArgumentException("No animation sequence loaded for " + location + ".");
        }
        return sequence;
    }

    public static AnimationSequence getAnimationSequenceOrNull(ResourceLocation location) {
        return ANIMATION_SEQUENCES.get(location);
    }

    @Override
    public CompletableFuture<Void> reload(SharedState sharedState, Executor backgroundExecutor, PreparationBarrier barrier, Executor gameExecutor) {
        ResourceManager resourceManager = sharedState.resourceManager();
        CompletableFuture<Map<ResourceLocation, JointSkeleton>> skeletons = loadJsonResources(resourceManager, backgroundExecutor, JointSkeleton.class, JOINT_SKELETON_PATH);
        CompletableFuture<Map<ResourceLocation, AnimationSequence>> sequences = loadJsonResources(resourceManager, backgroundExecutor, AnimationSequence.class, ANIMATION_SEQUENCE_PATH);

        return CompletableFuture.allOf(skeletons, sequences)
                .thenCompose(barrier::wait)
                .thenCompose(ignored -> CompletableFuture.runAsync(() -> {
                    JOINT_SKELETONS.clear();
                    JOINT_SKELETONS.putAll(skeletons.join());
                    ANIMATION_SEQUENCES.clear();
                    ANIMATION_SEQUENCES.putAll(sequences.join());
                    // Baking fills in identity timelines for joints a file left out, and needs the
                    // skeletons to already be in place.
                    ANIMATION_SEQUENCES.replaceAll((location, sequence) -> {
                        try {
                            return sequence.getBaked();
                        } catch (RuntimeException exception) {
                            LOGGER.warn("Could not bake animation sequence {}: {}", location, exception.getMessage());
                            return sequence;
                        }
                    });
                    LOGGER.info("Loaded {} joint skeleton(s) and {} animation sequence(s).", JOINT_SKELETONS.size(), ANIMATION_SEQUENCES.size());
                    com.locomotionslop.LocomotionSlop.reportPlayerModelPacks(resourceManager);
                }, gameExecutor));
    }

    @Override
    public String getName() {
        return "locomotion_slop_animation_assets";
    }

    private static <D> CompletableFuture<Map<ResourceLocation, D>> loadJsonResources(ResourceManager manager, Executor backgroundExecutor, Class<D> type, String pathToListFrom) {
        return CompletableFuture.supplyAsync(() -> {
            Predicate<ResourceLocation> isJson = location -> location.getPath().endsWith(".json");
            Map<ResourceLocation, Resource> foundResources = manager.listResources(pathToListFrom, isJson);

            Map<ResourceLocation, D> deserialized = new HashMap<>();
            foundResources.forEach((location, resource) -> {
                try (BufferedReader reader = resource.openAsReader()) {
                    JsonElement jsonElement = GsonHelper.fromJson(GsonConfiguration.getInstance(), reader, JsonElement.class);
                    if (jsonElement == null) {
                        LOGGER.warn("Skipping {} asset {}: empty file.", type.getSimpleName(), location);
                        return;
                    }
                    deserialized.put(location, GsonConfiguration.getInstance().fromJson(jsonElement, type));
                } catch (JsonParseException exception) {
                    LOGGER.warn("Skipping {} asset {} due to a parse error: {}", type.getSimpleName(), location, exception.getMessage());
                } catch (IOException exception) {
                    LOGGER.error("Could not read {} asset {}", type.getSimpleName(), location, exception);
                }
            });
            return deserialized;
        }, backgroundExecutor);
    }
}
