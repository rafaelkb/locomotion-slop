package com.locomotionslop.resource.json;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.locomotionslop.animation.joint.PartPoseData;
import org.joml.Vector3f;

import java.lang.reflect.Type;

public class PartPoseDataDeserializer implements JsonDeserializer<PartPoseData> {

    private static final String TRANSLATION_KEY = "translation";
    private static final String ROTATION_KEY = "rotation";
    private static final String SCALE_KEY = "scale";

    private static final Vector3f DEFAULT_TRANSLATION = new Vector3f(0.0F, 0.0F, 0.0F);
    private static final Vector3f DEFAULT_ROTATION = new Vector3f(0.0F, 0.0F, 0.0F);
    private static final Vector3f DEFAULT_SCALE = new Vector3f(1.0F, 1.0F, 1.0F);

    @Override
    public PartPoseData deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
        JsonObject json_object = json.getAsJsonObject();
        Vector3f translation = GsonConfiguration.deserializeWithFallback(context, json_object, TRANSLATION_KEY, Vector3f.class, DEFAULT_TRANSLATION);
        Vector3f rotation = GsonConfiguration.deserializeWithFallback(context, json_object, ROTATION_KEY, Vector3f.class, DEFAULT_ROTATION);
        Vector3f scale = GsonConfiguration.deserializeWithFallback(context, json_object, SCALE_KEY, Vector3f.class, DEFAULT_SCALE);
        return new PartPoseData(
                new float[]{translation.x(), translation.y(), translation.z()},
                new float[]{rotation.x(), rotation.y(), rotation.z()},
                new float[]{scale.x(), scale.y(), scale.z()}
        );
    }
}
