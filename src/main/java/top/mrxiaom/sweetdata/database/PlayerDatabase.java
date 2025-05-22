package top.mrxiaom.sweetdata.database;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.database.IDatabase;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweetdata.SweetData;
import top.mrxiaom.sweetdata.database.entry.PlayerCache;
import top.mrxiaom.sweetdata.func.AbstractPluginHolder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class PlayerDatabase extends AbstractPluginHolder implements IDatabase, Listener {
    private String TABLE_PLAYERS;
    private final Map<Player, PlayerCache> cacheMap = new HashMap<>();
    public PlayerDatabase(SweetData plugin) {
        super(plugin);
        registerEvents();
        long interval = 30 * 20L; // 每隔 30 秒检查一次，缓存是否需要提交
        plugin.getScheduler().runTaskTimerAsync(() -> {
            for (PlayerCache cache : cacheMap.values()) {
                if (cache.shouldSubmitCache()) {
                    Player player = cache.getPlayer();
                    List<Pair<String, String>> pairs = cache.getToSubmitPairs();
                    submitCache(player, pairs);
                }
            }
        }, interval, interval);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        cacheMap.remove(player);
        refreshCache(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        PlayerCache cache = cacheMap.remove(player);
        if (cache != null) {
            List<Pair<String, String>> pairs = cache.getToSubmitPairs();
            if (!pairs.isEmpty()) {
                submitCache(player, pairs);
            }
        }
    }

    @Override
    public void reload(Connection conn, String prefix) throws SQLException {
        TABLE_PLAYERS = prefix + "players";
        try (PreparedStatement ps = conn.prepareStatement(
                "CREATE TABLE if NOT EXISTS `" + TABLE_PLAYERS + "`(" +
                        "`player` VARCHAR(48)," + // 玩家名称 或 UUID
                        "`key` VARCHAR(64)," +    // 键
                        "`value` LONGTEXT," +     // 值
                        "PRIMARY KEY(`player`, `key`)" +
                ");"
        )) {
            ps.execute();
        }
    }

    public void submitCache(OfflinePlayer player, List<Pair<String, String>> pairs) {
        try (Connection conn = plugin.getConnection()) {
            String p = plugin.databaseKey(player);
            set(conn, p, pairs);
        } catch (SQLException e) {
            warn(e);
        }
    }

    @Nullable
    public PlayerCache getCacheOrNull(@Nullable OfflinePlayer p) {
        Player player = (p != null && p.isOnline()) ? p.getPlayer() : null;
        if (player != null) {
            return getCache(player);
        } else {
            return null;
        }
    }

    @NotNull
    public PlayerCache getCache(Player player) {
        PlayerCache cache = cacheMap.get(player);
        if (cache != null) {
            return cache;
        } else {
            return refreshCache(player);
        }
    }

    @NotNull
    public PlayerCache refreshCache(Player player) {
        PlayerCache cache = cacheMap.computeIfAbsent(player, id -> new PlayerCache(player));
        try (Connection conn = plugin.getConnection()) {
            String p = plugin.databaseKey(player);
            List<Pair<String, String>> pairs = get(conn, p);
            cache.putAll(pairs);
        } catch (SQLException e) {
            warn(e);
        }
        return cache;
    }

    @Nullable
    public Integer intAdd(OfflinePlayer player, String key, int toAdd) {
        try (Connection conn = plugin.getConnection()) {
            String p = plugin.databaseKey(player);
            Integer value = get(conn, p, key).flatMap(Util::parseInt).orElse(null);
            if (value != null) {
                int finalValue = value + toAdd;
                set(conn, p, key, String.valueOf(finalValue));
                return finalValue;
            }
        } catch (SQLException e) {
            warn(e);
        }
        return null;
    }

    public Optional<String> get(OfflinePlayer player, String key) {
        try (Connection conn = plugin.getConnection()) {
            String p = plugin.databaseKey(player);
            return get(conn, p, key);
        } catch (SQLException e) {
            warn(e);
        }
        return Optional.empty();
    }

    public void set(OfflinePlayer player, String key, String value) {
        try (Connection conn = plugin.getConnection()) {
            String p = plugin.databaseKey(player);
            set(conn, p, key, value);
        } catch (SQLException e) {
            warn(e);
        }
    }

    public void remove(OfflinePlayer player, String key) {
        try (Connection conn = plugin.getConnection()) {
            PlayerCache cache = getCacheOrNull(player);
            if (cache != null) {
                cache.remove(key);
            }
            String p = plugin.databaseKey(player);
            remove(conn, p, key);
        } catch (SQLException e) {
            warn(e);
        }
    }

    @NotNull
    private Optional<String> get(Connection conn, String player, String key) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT `value` FROM `" + TABLE_PLAYERS + "` WHERE `player=`? AND `key`=?;"
        )) {
            ps.setString(1, player);
            ps.setString(2, key);
            try (ResultSet result = ps.executeQuery()) {
                if (result.next()) {
                    String value = result.getString("value");
                    return Optional.of(value);
                }
            }
        }
        return Optional.empty();
    }

    @NotNull
    private List<Pair<String, String>> get(Connection conn, String player) throws SQLException {
        List<Pair<String, String>> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM `" + TABLE_PLAYERS + "` WHERE `player`=?;"
        )) {
            ps.setString(1, player);
            try (ResultSet result = ps.executeQuery()) {
                while (result.next()) {
                    String key = result.getString("key");
                    String value = result.getString("value");
                    list.add(Pair.of(key, value));
                }
            }
        }
        return list;
    }

    private void set(Connection conn, String player, String key, String value) throws SQLException {
        String statement;
        boolean mySQL = plugin.options.database().isMySQL();
        if (mySQL) {
            statement = "INSERT INTO `" + TABLE_PLAYERS + "`(`player`,`key`,`value`) VALUES(?, ?, ?) on duplicate key update `value`=?;";
        } else {
            statement = "INSERT OR REPLACE INTO `" + TABLE_PLAYERS + "`(`player`,`key`,`value`) VALUES(?, ?, ?);";
        }
        try (PreparedStatement ps = conn.prepareStatement(statement)) {
            ps.setString(1, player);
            ps.setString(2, key);
            ps.setString(3, value);
            if (mySQL) {
                ps.setString(4, value);
            }
            ps.execute();
        }
    }

    private void set(Connection conn, String player, List<Pair<String, String>> pairs) throws SQLException {
        String statement;
        boolean mySQL = plugin.options.database().isMySQL();
        if (mySQL) {
            statement = "INSERT INTO `" + TABLE_PLAYERS + "`(`player`,`key`,`value`) VALUES(?, ?, ?) on duplicate key update `value`=?;";
        } else {
            statement = "INSERT OR REPLACE INTO `" + TABLE_PLAYERS + "`(`player`,`key`,`value`) VALUES(?, ?, ?);";
        }
        try (PreparedStatement ps = conn.prepareStatement(statement)) {
            for (Pair<String, String> pair : pairs) {
                ps.setString(1, player);
                ps.setString(2, pair.key());
                ps.setString(3, pair.value());
                if (mySQL) {
                    ps.setString(4, pair.value());
                }
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void remove(Connection conn, String player, String key) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM `" + TABLE_PLAYERS + "` WHERE `player`=? AND `key`=?;"
        )) {
            ps.setString(1, player);
            ps.setString(2, key);
            ps.execute();
        }
    }
}
