package net.eltown.servercore.components.api.intern;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.data.settings.AccountSettings;
import net.eltown.servercore.components.data.settings.SettingsCalls;
import net.eltown.servercore.components.tinyrabbit.Queue;

import java.util.*;
import java.util.function.Consumer;

public record SettingsAPI(ServerCore serverCore) {

    public static final HashMap<String, AccountSettings> cachedSettings = new HashMap<>();
    public static final List<String> hasChanges = new ArrayList<>();

    public void updateSettingsDirect(final String player, final String key, final String value) {
        final Map<String, String> map = cachedSettings.get(player).getSettings();
        map.remove(key);
        map.put(key, value);
        cachedSettings.get(player).setSettings(map);

        this.serverCore.getTinyRabbit().send(Queue.SETTINGS_RECEIVE, SettingsCalls.REQUEST_UPDATE_SETTINGS.name(), player, key, value);
    }

    public void updateSettings(final String player, final String key, final String value) {
        final Map<String, String> map = cachedSettings.get(player).getSettings();
        map.remove(key);
        map.put(key, value);
        cachedSettings.get(player).setSettings(map);
        if (!hasChanges.contains(player)) hasChanges.add(player);
    }

    public void updateAll(final String player) {
        final StringBuilder stringBuilder = new StringBuilder();
        cachedSettings.get(player).getSettings().forEach((k, v) -> {
            stringBuilder.append(k).append(":").append(v).append(">:<");
        });
        final String settings = stringBuilder.substring(0, stringBuilder.length() - 3);

        this.serverCore.getTinyRabbit().send(Queue.SETTINGS_RECEIVE, SettingsCalls.REQUEST_UPDATE_ALL.name(), player, settings);
    }

    public void removeEntry(final String player, final String key) {
        cachedSettings.get(player).getSettings().remove(key);

        this.serverCore.getTinyRabbit().send(Queue.SETTINGS_RECEIVE, SettingsCalls.REQUEST_REMOVE_SETTINGS.name(), player, key);
    }

    public void getEntry(final String player, final String key, final String def, final Consumer<String> value) {
        if (!cachedSettings.containsKey(player)) {
            this.serverCore.getTinyRabbit().sendAndReceive(delivery -> {
                switch (SettingsCalls.valueOf(delivery.getKey().toUpperCase())) {
                    case CALLBACK_SETTINGS:
                        if (!delivery.getData()[1].equals("null")) {
                            final Map<String, String> map = new HashMap<>();
                            final List<String> list = Arrays.asList(delivery.getData()[1].split(">:<"));
                            list.forEach(e -> {
                                map.put(e.split(":")[0], e.split(":")[1]);
                            });
                            cachedSettings.put(player, new AccountSettings(player, map));
                            value.accept(map.getOrDefault(key, def));
                            return;
                        } else cachedSettings.put(player, new AccountSettings(player, new HashMap<>()));
                        break;
                }
            }, Queue.SETTINGS_CALLBACK, SettingsCalls.REQUEST_SETTINGS.name(), player);
        }
        value.accept(cachedSettings.get(player).getSettings().getOrDefault(key, def));
    }

}
