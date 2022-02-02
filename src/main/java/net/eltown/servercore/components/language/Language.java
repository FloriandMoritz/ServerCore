package net.eltown.servercore.components.language;

import lombok.SneakyThrows;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.config.Config;

import java.util.HashMap;
import java.util.Map;

public class Language {

    private static final Map<String, String> messages = new HashMap<>();
    private static String prefix;

    @SneakyThrows
    public static void init(ServerCore plugin) {
        messages.clear();
        Config.saveResource("messages.yml", plugin);
        Config m = new Config(plugin.getDataFolder() + "/messages.yml");
        for (Map.Entry<String, Object> map : m.getAll().entrySet()) {
            String key = map.getKey();
            if (map.getValue() instanceof String val) {
                messages.put(key, val);
            }
        }
        prefix = m.getString("prefix");
    }

    public static String get(String key, Object... replacements) {
        String message = prefix.replace("&", "§") + messages.getOrDefault(key, "null").replace("&", "§");

        int i = 0;
        for (Object replacement : replacements) {
            message = message.replace("[" + i + "]", String.valueOf(replacement));
            i++;
        }

        return message;
    }

    public static String getNP(String key, Object... replacements) {
        String message = messages.getOrDefault(key, "null").replace("&", "§");

        int i = 0;
        for (Object replacement : replacements) {
            message = message.replace("[" + i + "]", String.valueOf(replacement));
            i++;
        }

        return message;
    }

}
