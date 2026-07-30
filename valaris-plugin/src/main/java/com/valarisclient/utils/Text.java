package com.valarisclient.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class Text {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private Text() {
    }

    public static Component mm(String mini) {
        if (mini == null || mini.isBlank()) {
            return Component.empty();
        }
        return MM.deserialize(mini);
    }

    public static void send(Player player, String mini) {
        if (player != null) {
            player.sendMessage(mm(mini));
        }
    }

    public static String applyPlayer(String input, Player player) {
        if (input == null) {
            return "";
        }
        return input
                .replace("%player%", player.getName())
                .replace("%uuid%", player.getUniqueId().toString())
                .replace("%uuid_nodash%", player.getUniqueId().toString().replace("-", ""));
    }

    public static JsonObject parseJson(String json) {
        try {
            return JsonParser.parseString(json).getAsJsonObject();
        } catch (Exception e) {
            return new JsonObject();
        }
    }

    public static String str(JsonObject o, String key, String def) {
        if (o == null || !o.has(key) || o.get(key).isJsonNull()) {
            return def;
        }
        try {
            return o.get(key).getAsString();
        } catch (Exception e) {
            return def;
        }
    }

    public static int integer(JsonObject o, String key, int def) {
        if (o == null || !o.has(key) || o.get(key).isJsonNull()) {
            return def;
        }
        try {
            return o.get(key).getAsInt();
        } catch (Exception e) {
            return def;
        }
    }

    /**
     * Decode Fabric {@code STRING_UTF8} (VarInt length + UTF-8) or raw UTF-8 JSON.
     */
    public static String decodePayload(byte[] data) {
        if (data == null || data.length == 0) {
            return "";
        }
        try {
            int[] idx = {0};
            int len = readVarInt(data, idx);
            if (len >= 0 && idx[0] + len <= data.length) {
                String s = new String(data, idx[0], len, StandardCharsets.UTF_8);
                if (s.startsWith("{")) {
                    return s;
                }
            }
        } catch (Exception ignored) {
        }
        String raw = new String(data, StandardCharsets.UTF_8).trim();
        int brace = raw.indexOf('{');
        return brace >= 0 ? raw.substring(brace) : raw;
    }

    public static byte[] encodePayload(String json) {
        byte[] utf8 = json.getBytes(StandardCharsets.UTF_8);
        byte[] len = writeVarInt(utf8.length);
        byte[] out = new byte[len.length + utf8.length];
        System.arraycopy(len, 0, out, 0, len.length);
        System.arraycopy(utf8, 0, out, len.length, utf8.length);
        return out;
    }

    private static int readVarInt(byte[] buf, int[] index) {
        int value = 0;
        int position = 0;
        while (true) {
            if (index[0] >= buf.length) {
                throw new IllegalArgumentException("VarInt overrun");
            }
            byte current = buf[index[0]++];
            value |= (current & 0x7F) << position;
            if ((current & 0x80) == 0) {
                break;
            }
            position += 7;
            if (position >= 32) {
                throw new IllegalArgumentException("VarInt too big");
            }
        }
        return value;
    }

    private static byte[] writeVarInt(int value) {
        byte[] tmp = new byte[5];
        int i = 0;
        while ((value & ~0x7F) != 0) {
            tmp[i++] = (byte) ((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        tmp[i++] = (byte) value;
        byte[] out = new byte[i];
        System.arraycopy(tmp, 0, out, 0, i);
        return out;
    }

    public static UUID parseUuid(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String s = raw.trim();
        if (s.length() == 32) {
            s = s.replaceFirst(
                    "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
                    "$1-$2-$3-$4-$5");
        }
        try {
            return UUID.fromString(s);
        } catch (Exception e) {
            return null;
        }
    }

    public static int levelForXp(int xp) {
        int level = 0;
        int remaining = Math.max(0, xp);
        while (remaining >= xpForLevel(level) && level < 999) {
            remaining -= xpForLevel(level);
            level++;
        }
        return level;
    }

    public static int xpForLevel(int level) {
        return 100 + Math.max(0, level) * 25;
    }
}
