package top.mrxiaom.sweetdata;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.sweetdata.database.PlayerDatabase;
import top.mrxiaom.sweetdata.database.entry.PlayerCache;

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
            PlayerDatabase db = plugin.getPlayerDatabase();
            String[] split = params.substring(4).split(";", 2);
            String key = split[0];
            String def = split.length == 2 ? split[1] : "";
            PlayerCache cache = db.getCacheOrNull(player);
            if (cache != null) {
                return cache.get(key).orElse(def);
            } else {
                return db.get(player, key).orElse(def);
            }
        }
        return null;
    }
}
