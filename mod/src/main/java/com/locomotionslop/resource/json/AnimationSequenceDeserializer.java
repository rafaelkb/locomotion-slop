/*
 * Locomotion Slop - AnimationSequenceDeserializer
 * Ported from Locomotion (GPLv3).
 */
package com.locomotionslop.resource.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.locomotionslop.animation.sequence.AnimationSequence;
import com.locomotionslop.animation.util.Interpolator;
import com.locomotionslop.animation.util.TimeSpan;
import com.locomotionslop.animation.util.Timeline;
import com.locomotionslop.resource.FormatVersion;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.lang.reflect.Type;
import java.util.List;

public class AnimationSequenceDeserializer implements JsonDeserializer<AnimationSequence> {

    private static final String LENGTH_KEY = "length";
    private static final String JOINT_SKELETON_KEY = "joint_skeleton";
    private static final String JOINT_CHANNELS_KEY = "joint_channels";
    private static final String CUSTOM_ATTRIBUTES_KEY = "custom_attributes";
    private static final String TIME_MARKERS_KEY = "time_markers";

    private static final List<String> REQUIRED_KEYS = List.of(LENGTH_KEY, JOINT_CHANNELS_KEY);

    @Override
    public AnimationSequence deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
        JsonObject sequenceJson = json.getAsJsonObject();

        if (FormatVersion.ofAssetJsonObject(sequenceJson).isIncompatible()) {
            throw new JsonParseException("Animation sequence format version is older than this mod supports (needs " + FormatVersion.SUPPORTED + "+).");
        }
        for (String key : REQUIRED_KEYS) {
            if (!sequenceJson.has(key)) {
                throw new JsonParseException("Required key " + key + " not found in sequence data.");
            }
        }

        ResourceLocation jointSkeletonLocation = ResourceLocation.tryParse(sequenceJson.get(JOINT_SKELETON_KEY).getAsString());
        if (jointSkeletonLocation == null) {
            throw new JsonParseException("Joint skeleton id \"" + sequenceJson.get(JOINT_SKELETON_KEY).getAsString() + "\" is invalid.");
        }
        // Skeleton files live under assets/<namespace>/skeletons/<path>.json and are indexed by
        // "<namespace>:<path>.json", which is how ResourceManager#listResources keys them.
        jointSkeletonLocation = jointSkeletonLocation.withPath(path -> "skeletons/" + path + ".json");

        float sequenceLength = sequenceJson.get(LENGTH_KEY).getAsFloat();
        AnimationSequence.Builder sequenceBuilder = AnimationSequence.builder(TimeSpan.ofSeconds(sequenceLength), jointSkeletonLocation);

        JsonObject jointChannelsJson = sequenceJson.getAsJsonObject(JOINT_CHANNELS_KEY);
        jointChannelsJson.asMap().forEach((joint, jointElement) -> {
            JsonObject jointJson = jointElement.getAsJsonObject();
            sequenceBuilder.putJointTranslationTimeline(joint, deserializeTimeline(context, jointJson, "translation", Vector3f.class, Interpolator.VECTOR_FLOAT, sequenceLength));
            sequenceBuilder.putJointRotationTimeline(joint, deserializeTimeline(context, jointJson, "rotation", Quaternionf.class, Interpolator.QUATERNION, sequenceLength));
            sequenceBuilder.putJointScaleTimeline(joint, deserializeTimeline(context, jointJson, "scale", Vector3f.class, Interpolator.VECTOR_FLOAT, sequenceLength));
            sequenceBuilder.putJointVisibilityTimeline(joint, deserializeTimeline(context, jointJson, "visibility", Boolean.class, Interpolator.BOOLEAN_KEYFRAME, sequenceLength));
        });

        if (sequenceJson.has(CUSTOM_ATTRIBUTES_KEY)) {
            JsonObject customAttributesJson = sequenceJson.getAsJsonObject(CUSTOM_ATTRIBUTES_KEY);
            customAttributesJson.asMap().forEach((attribute, element) ->
                    sequenceBuilder.putCustomAttributeTimeline(attribute, deserializeTimeline(context, customAttributesJson, attribute, Float.class, Interpolator.FLOAT, sequenceLength)));
        }

        if (sequenceJson.has(TIME_MARKERS_KEY)) {
            sequenceJson.getAsJsonObject(TIME_MARKERS_KEY).asMap().forEach((marker, element) -> {
                JsonArray times = element.getAsJsonArray();
                times.forEach(time -> sequenceBuilder.putTimeMarker(marker, TimeSpan.ofSeconds(time.getAsFloat())));
            });
        }
        return sequenceBuilder.build();
    }

    private static <X> Timeline<X> deserializeTimeline(JsonDeserializationContext context, JsonObject jsonObject, String identifier, Class<X> type, Interpolator<X> interpolator, float sequenceLength) {
        Timeline<X> timeline = Timeline.of(interpolator, sequenceLength);
        if (!jsonObject.has(identifier)) {
            return timeline;
        }
        jsonObject.getAsJsonObject(identifier).asMap().forEach((keyframeString, keyframeValue) -> {
            float keyframe = Float.parseFloat(keyframeString);
            timeline.addKeyframe(keyframe, context.deserialize(keyframeValue, type));
        });
        return timeline;
    }
}
