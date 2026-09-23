/*
 * Locomotion Slop - JointChannelDeserializer
 * Ported from Locomotion (GPLv3).
 */
package com.locomotionslop.resource.json;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.locomotionslop.animation.joint.JointChannel;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.lang.reflect.Type;

public class JointChannelDeserializer implements JsonDeserializer<JointChannel> {

    private static final String TRANSLATION_KEY = "translation";
    private static final String ROTATION_KEY = "rotation";
    private static final String SCALE_KEY = "scale";
    private static final String VISIBILITY_KEY = "visibility";

    private static final Vector3f DEFAULT_TRANSLATION = new Vector3f(0.0F, 0.0F, 0.0F);
    private static final Quaternionf DEFAULT_ROTATION = new Quaternionf();
    private static final Vector3f DEFAULT_SCALE = new Vector3f(1.0F, 1.0F, 1.0F);
    private static final boolean DEFAULT_VISIBILITY = true;

    @Override
    public JointChannel deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
        JsonObject channelJson = json.getAsJsonObject();
        return JointChannel.ofTranslationRotationScaleQuaternion(
                GsonConfiguration.deserializeWithFallback(context, channelJson, TRANSLATION_KEY, Vector3f.class, DEFAULT_TRANSLATION),
                GsonConfiguration.deserializeWithFallback(context, channelJson, ROTATION_KEY, Quaternionf.class, DEFAULT_ROTATION),
                GsonConfiguration.deserializeWithFallback(context, channelJson, SCALE_KEY, Vector3f.class, DEFAULT_SCALE),
                Boolean.TRUE.equals(GsonConfiguration.deserializeWithFallback(context, channelJson, VISIBILITY_KEY, Boolean.class, DEFAULT_VISIBILITY))
        );
    }
}
