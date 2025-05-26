package top.mrxiaom.sweetdata;

import me.clip.placeholderapi.PlaceholderAPIPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweetdata.database.PlayerDatabase;
import top.mrxiaom.sweetdata.database.entry.PlayerCache;

import java.util.Optional;

public class Placeholders extends PlaceholderExpansion {
    private final SweetData plugin;
    public Placeholders(SweetData plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean register() {
        try {
            unregister();
        } catch (Throwable ignored) {}
        return super.register();
    }

    @Override
    public @NotNull String getIdentifier() {
        return plugin.getDescription().getName().toLowerCase();
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (params.startsWith("global_")) { // TODO: 全局数值

        }
        String request = super.onRequest(player, params);
        if (request != null) {
            return request;
        }
        if (player != null) { // 玩家数值
            if (params.startsWith("$")) {
                String[] split = params.substring(1).split("\\$", 2);
                if (split.length == 1) return bool(false);
                String[] conditionArray = split[0].split(",");

                String[] split1 = split[1].split(";", 2);
                String key = split1[0];
                String def = split1.length == 2 ? split1[1] : "";

                if ("range".equals(conditionArray[0]) && conditionArray.length == 3) {
                    Integer rangeMin = Util.parseInt(conditionArray[1]).orElse(null);
                    Integer rangeMax = Util.parseInt(conditionArray[2]).orElse(null);
                    if (rangeMin == null || rangeMax == null) return bool(false);
                    String str = get(player, key).orElse(def);
                    Integer value = Util.parseInt(str).orElse(null);
                    if (value == null) return bool(false);
                    return bool(value >= rangeMin && value <= rangeMax);
                }
                return bool(false);
            }

            String[] split = params.split(";", 2);
            String key = split[0];
            String def = split.length == 2 ? split[1] : "";

            return get(player, key).orElse(def);
        }
        return null;
    }

    private Optional<String> get(@NotNull OfflinePlayer player, String key) {
        PlayerDatabase db = plugin.getPlayerDatabase();
        PlayerCache cache = db.getCacheOrNull(player);
        if (cache != null) {
            return cache.get(key);
        } else {
            return db.get(player, key);
        }
    }

    public static String bool(boolean value) {
        return value ? PlaceholderAPIPlugin.booleanTrue() : PlaceholderAPIPlugin.booleanFalse();
    }
}
