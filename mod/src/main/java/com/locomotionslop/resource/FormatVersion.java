package com.locomotionslop.resource;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

/**
 * The {@code format_version} field at the top of every skeleton and sequence file.
 * Locomotion's current export format is version 5.
 */
public record FormatVersion(int version) {

    public static final String KEY = "format_version";
    public static final int SUPPORTED = 5;

    public static FormatVersion of(int version) {
        return new FormatVersion(version);
    }

    public boolean isIncompatible() {
        return this.version < SUPPORTED;
    }

    public static FormatVersion ofAssetJsonObject(JsonObject assetJson) {
        if (assetJson.has(KEY)) {
            return FormatVersion.of(assetJson.get(KEY).getAsInt());
        }
        throw new JsonParseException("Asset does not contain a " + KEY + " field.");
    }
}
