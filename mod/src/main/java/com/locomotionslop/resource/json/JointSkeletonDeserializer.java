/*
 * Locomotion Slop - JointSkeletonDeserializer
 * Ported from Locomotion (GPLv3).
 */
package com.locomotionslop.resource.json;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.locomotionslop.animation.joint.JointChannel;
import com.locomotionslop.animation.joint.PartPoseData;
import com.locomotionslop.animation.joint.skeleton.JointSkeleton;
import com.locomotionslop.resource.FormatVersion;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class JointSkeletonDeserializer implements JsonDeserializer<JointSkeleton> {

    private static final String ROOT_KEY = "root_joint";
    private static final String JOINTS_KEY = "joints";
    private static final String CUSTOM_ATTRIBUTE_KEY = "custom_attributes";
    private static final String CUSTOM_ATTRIBUTE_TYPE_KEY = "type";
    private static final String CUSTOM_ATTRIBUTE_DEFAULT_VALUE_KEY = "default_value";

    private static final String CHILDREN_KEY = "children";
    private static final String MIRROR_JOINT_KEY = "mirror_joint";
    private static final String MODEL_PART_IDENTIFIER_KEY = "model_part_identifier";
    private static final String REFERENCE_POSE_KEY = "reference_pose";
    private static final String MODEL_PART_OFFSET_KEY = "model_part_offset";
    private static final String MODEL_PART_SPACE_PARENT_KEY = "model_part_space_parent";

    private static final List<String> REQUIRED_SKELETON_KEYS = List.of(ROOT_KEY, JOINTS_KEY);
    private static final List<String> REQUIRED_JOINT_KEYS = List.of(CHILDREN_KEY, REFERENCE_POSE_KEY);

    @Override
    public JointSkeleton deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
        JsonObject skeletonJson = json.getAsJsonObject();

        if (FormatVersion.ofAssetJsonObject(skeletonJson).isIncompatible()) {
            throw new JsonParseException("Joint skeleton format version is older than this mod supports (needs " + FormatVersion.SUPPORTED + "+).");
        }
        for (String key : REQUIRED_SKELETON_KEYS) {
            if (!skeletonJson.has(key)) {
                throw new JsonParseException("Required key " + key + " not found in skeleton data.");
            }
        }

        JointSkeleton.Builder skeletonBuilder = JointSkeleton.of(skeletonJson.get(ROOT_KEY).getAsString());
        Map<String, JsonElement> jointsJsonMap = skeletonJson.get(JOINTS_KEY).getAsJsonObject().asMap();
        this.deserializeJointAndChildren(skeletonJson.get(ROOT_KEY).getAsString(), null, jointsJsonMap, context, skeletonBuilder);

        if (skeletonJson.has(CUSTOM_ATTRIBUTE_KEY)) {
            skeletonJson.get(CUSTOM_ATTRIBUTE_KEY).getAsJsonObject().asMap().forEach((name, attributeJson) -> {
                JsonObject attributeObject = attributeJson.getAsJsonObject();
                if (Objects.equals(attributeObject.get(CUSTOM_ATTRIBUTE_TYPE_KEY).getAsString(), "float")) {
                    skeletonBuilder.defineCustomAttribute(name, attributeObject.get(CUSTOM_ATTRIBUTE_DEFAULT_VALUE_KEY).getAsFloat());
                }
            });
        }
        return skeletonBuilder.build();
    }

    private void deserializeJointAndChildren(
            String joint,
            @Nullable String parent,
            Map<String, JsonElement> jointsJsonMap,
            JsonDeserializationContext context,
            JointSkeleton.Builder skeletonBuilder
    ) {
        if (!jointsJsonMap.containsKey(joint)) {
            throw new JsonParseException("Joint \"" + joint + "\" being defined is not present in the skeleton.");
        }
        JointSkeleton.JointConfiguration.Builder jointBuilder = JointSkeleton.JointConfiguration.builder();
        JsonObject jointJson = jointsJsonMap.get(joint).getAsJsonObject();
        for (String key : REQUIRED_JOINT_KEYS) {
            if (!jointJson.has(key)) {
                throw new JsonParseException("Required key " + key + " not found in joint data.");
            }
        }
        if (parent != null && !jointsJsonMap.containsKey(parent)) {
            throw new JsonParseException("Joint \"" + joint + "\" has parent \"" + parent + "\" that is not present in the skeleton.");
        }

        for (JsonElement childJson : jointJson.get(CHILDREN_KEY).getAsJsonArray()) {
            String child = childJson.getAsString();
            if (!jointsJsonMap.containsKey(child)) {
                throw new JsonParseException("Joint \"" + joint + "\" has child \"" + child + "\" that is not present in the skeleton.");
            }
            jointBuilder.addChild(child);
            this.deserializeJointAndChildren(child, joint, jointsJsonMap, context, skeletonBuilder);
        }

        jointBuilder.setParent(parent);
        jointBuilder.setReferencePose(GsonConfiguration.deserializeWithFallback(context, jointJson, REFERENCE_POSE_KEY, JointChannel.class, JointChannel.ZERO));
        jointBuilder.setMirrorJoint(GsonConfiguration.deserializeWithFallback(context, jointJson, MIRROR_JOINT_KEY, String.class, null));
        jointBuilder.setModelPartIdentifier(GsonConfiguration.deserializeWithFallback(context, jointJson, MODEL_PART_IDENTIFIER_KEY, String.class, null));
        jointBuilder.setModelPartOffset(GsonConfiguration.deserializeWithFallback(context, jointJson, MODEL_PART_OFFSET_KEY, PartPoseData.class, PartPoseData.IDENTITY));

        String modelPartSpaceParent = GsonConfiguration.deserializeWithFallback(context, jointJson, MODEL_PART_SPACE_PARENT_KEY, String.class, null);
        if (modelPartSpaceParent != null && jointsJsonMap.containsKey(modelPartSpaceParent)) {
            jointBuilder.setModelPartSpaceParent(modelPartSpaceParent);
        }

        skeletonBuilder.defineJoint(joint, jointBuilder.build());
    }
}
