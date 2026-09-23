/*
 * Locomotion Slop - GsonConfiguration
 * Ported from Locomotion (GPLv3).
 */
package com.locomotionslop.resource.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;
import com.locomotionslop.animation.joint.JointChannel;
import com.locomotionslop.animation.joint.PartPoseData;
import com.locomotionslop.animation.joint.skeleton.JointSkeleton;
import com.locomotionslop.animation.sequence.AnimationSequence;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The Gson instance used for every skeleton and sequence file.
 */
public final class GsonConfiguration {

    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Vector3f.class, vector3fDeserializer())
            .registerTypeAdapter(Quaternionf.class, quaternionDeserializer())
            .registerTypeAdapter(PartPoseData.class, new PartPoseDataDeserializer())
            .registerTypeAdapter(JointChannel.class, new JointChannelDeserializer())
            .registerTypeAdapter(JointSkeleton.class, new JointSkeletonDeserializer())
            .registerTypeAdapter(AnimationSequence.class, new AnimationSequenceDeserializer())
            .create();

    private GsonConfiguration() {
    }

    public static Gson getInstance() {
        return GSON;
    }

    public static <D> D deserializeWithFallback(JsonDeserializationContext context, JsonObject json, String key, Class<D> type, D fallback) {
        if (!json.has(key) || json.get(key).isJsonNull()) {
            return fallback;
        }
        return context.deserialize(json.get(key), type);
    }

    private static JsonDeserializer<Vector3f> vector3fDeserializer() {
        return (jsonElement, type, context) -> {
            JsonArray components = jsonElement.getAsJsonArray();
            return new Vector3f(
                    components.get(0).getAsFloat(),
                    components.get(1).getAsFloat(),
                    components.get(2).getAsFloat()
            );
        };
    }

    /**
     * Rotations are stored as ZYX euler angles in <b>degrees</b>, which joml wants in radians.
     */
    private static JsonDeserializer<Quaternionf> quaternionDeserializer() {
        return (jsonElement, type, context) -> {
            JsonArray components = jsonElement.getAsJsonArray();
            return new Quaternionf().rotationZYX(
                    components.get(2).getAsFloat() * DEG_TO_RAD,
                    components.get(1).getAsFloat() * DEG_TO_RAD,
                    components.get(0).getAsFloat() * DEG_TO_RAD
            );
        };
    }
}
