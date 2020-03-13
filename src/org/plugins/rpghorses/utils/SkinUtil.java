package org.plugins.rpghorses.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.UUID;

public class SkinUtil {

    private static JsonParser parser = new JsonParser();

    public static UUID getUUIDFromName(String name) {
        try {
            URL url_0 = new URL("https://api.mojang.com/users/profiles/minecraft/" + name);
            InputStreamReader reader_0 = new InputStreamReader(url_0.openStream());
            String uuidString = SkinUtil.parser.parse(reader_0).getAsJsonObject().get("id").getAsString();
            uuidString = uuidString.substring(0, 8) + "-" + uuidString.substring(8, 12) + "-" + uuidString.substring(12, 16) + "-" + uuidString.substring(16, 20) + "-" + uuidString.substring(20);
            return UUID.fromString(uuidString);
        } catch (Exception e) {
            return null;
        }
    }

    public static String[] getValueAndSignature(UUID uuid) {
        try {
            String uuidStr = uuid.toString().replace("-", "");
            URL url_1 = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuidStr + "?unsigned=false");
            InputStreamReader reader_1 = new InputStreamReader(url_1.openStream());
            JsonObject textureProperty = new JsonParser().parse(reader_1).getAsJsonObject().get("properties").getAsJsonArray().get(0).getAsJsonObject();
            String texture = textureProperty.get("value").getAsString();
            String signature = textureProperty.get("signature").getAsString();
            String[] args = new String[2];
            args[0] = texture;
            args[1] = signature;
            return args;
        } catch (Exception e) {
            return null;
        }
    }
}
