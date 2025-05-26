package top.mrxiaom.sweetdata.commands;
        
import com.google.common.collect.Lists;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweetdata.SweetData;
import top.mrxiaom.sweetdata.database.PlayerDatabase;
import top.mrxiaom.sweetdata.database.entry.PlayerCache;
import top.mrxiaom.sweetdata.func.AbstractModule;

import java.util.*;

@AutoRegister
public class CommandMain extends AbstractModule implements CommandExecutor, TabCompleter, Listener {
    public CommandMain(SweetData plugin) {
        super(plugin);
        registerCommand("sweetdata", this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender.isOp()) {
            if (operatorCommand(sender, args)) return true;
        }
        return true;
    }

    private boolean operatorCommand(CommandSender sender, String[] args) {
        if (args.length >= 3 && "get".equalsIgnoreCase(args[0])) {
            OfflinePlayer player = Util.getOfflinePlayer(args[1]).orElse(null);
            if (player == null) {
                return t(sender, "&e指定的玩家不存在 &7(" + args[1] + ")");
            }
            String key = args[2];
            String value;

            PlayerDatabase db = plugin.getPlayerDatabase();
            PlayerCache cache = db.getCacheOrNull(player);
            if (cache != null) {
                value = cache.get(key).orElse(null);
            } else {
                value = db.get(player, key).orElse(null);
            }
            if (value != null) {
                return t(sender, "&a玩家&e " + args[1] + " &a的数值 &e" + key + "=" + value + "&a.");
            } else {
                return t(sender, "&a玩家&e " + args[1] + " &a没有设置数值&e " + key + "&a.");
            }
        }
        if (args.length >= 4 && "set".equalsIgnoreCase(args[0])) {
            OfflinePlayer player = Util.getOfflinePlayer(args[1]).orElse(null);
            if (player == null) {
                return t(sender, "&e指定的玩家不存在 &7(" + args[1] + ")");
            }
            String key = args[2];
            String value = consume(args, 3);

            PlayerDatabase db = plugin.getPlayerDatabase();
            PlayerCache cache = db.getCacheOrNull(player);
            if (cache != null) {
                cache.put(key, value);
                cache.setNextSubmitAfter(30 * 1000L, false);
            } else {
                db.set(player, key, value);
            }
            return t(sender, "&a已设置玩家&e " + args[1] + " &a的数值&e " + key + "=" + value + "&a.");
        }
        if (args.length >= 3 && ("remove".equalsIgnoreCase(args[0]) || "del".equalsIgnoreCase(args[0]))) {
            OfflinePlayer player = Util.getOfflinePlayer(args[1]).orElse(null);
            if (player == null) {
                return t(sender, "&e指定的玩家不存在 &7(" + args[1] + ")");
            }
            String key = args[2];

            PlayerDatabase db = plugin.getPlayerDatabase();
            PlayerCache cache = db.getCacheOrNull(player);
            if (cache != null) {
                cache.remove(key);
            }
            db.remove(player, key);
            return t(sender, "&a已移除玩家&e " + args[1] + " &a的数值 &e" + key + "&a.");
        }
        if (args.length >= 4 && "plus".equalsIgnoreCase(args[0])) {
            OfflinePlayer player = Util.getOfflinePlayer(args[1]).orElse(null);
            if (player == null) {
                return t(sender, "&e指定的玩家不存在 &7(" + args[1] + ")");
            }
            String key = args[2];
            Integer toAdd = Util.parseInt(args[3]).orElse(null);
            if (toAdd == null) {
                return t(sender, "&e无效的数值 " + args[3]);
            }
            PlayerDatabase db = plugin.getPlayerDatabase();
            PlayerCache cache = db.getCacheOrNull(player);
            Integer result = null;
            if (cache != null) {
                Integer value = cache.getInt(key).orElse(null);
                if (value != null) {
                    result = value + toAdd;
                    cache.put(key, result);
                    cache.setNextSubmitAfter(30 * 1000L, false);
                }
            } else {
                result = db.intAdd(player, key, toAdd);
            }
            if (result != null) {
                return t(sender, "&a已设置玩家&e " + args[1] + " &a的数值增加&e " + toAdd + "&a，最终&e " + key + "&f=&e" + result + "&a.");
            }
            // return t(sender, "&e为玩家&b " + args[1] + " &e的数值&b " + key + " &a增加&e " + toAdd + " &a失败");
            return true;
        }
        if (args.length > 0 && "reload".equalsIgnoreCase(args[0])) {
            if (args.length > 1 && "database".equalsIgnoreCase(args[1])) {
                plugin.options.database().reloadConfig();
                plugin.options.database().reconnect();
                PlayerDatabase db = plugin.getPlayerDatabase();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    db.refreshCache(player);
                }
                return t(sender, "&a已重新连接数据库");
            }
            plugin.reloadConfig();
            return t(sender, "&a配置文件已重载");
        }
        return t(sender, "&e&lSweetData 玩家数据命令&r",
                "  &f/data get <玩家名> <键> &7获取玩家的数值",
                "  &f/data set <玩家名> <键> <值> &7设置玩家的数值",
                "  &f/data plus <玩家名> <键> <值> &7如果数值是整数，增加玩家的数值(可以为负数)，如果数值不是整数或不存在，不进行任何操作",
                "  &f/data remove <玩家名> <键> &7移除玩家的数值",
                "  &f/data del <玩家名> <键> &7移除玩家的数值",
                "&e&lSweetData 管理命令&r",
                "  &f/data reload database &7重新连接数据库，并刷新所有缓存",
                "  &f/data reload &7重载配置文件");
    }

    private static final List<String> emptyList = Lists.newArrayList();
    private static final List<String> listArg0 = Lists.newArrayList();
    private static final List<String> listOpArg0 = Lists.newArrayList(
            "get", "set", "plus", "remove", "del", "reload");
    private static final List<String> listArg1Player = Lists.newArrayList(
            "get", "set", "plus", "remove", "del");
    private static final List<String> listArg1Reload = Lists.newArrayList(
            "database");
    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return startsWith(sender.isOp() ? listOpArg0 : listArg0, args[0]);
        }
        if (args.length == 2) {
            if (sender.isOp()) {
                if (listArg1Player.contains(args[0].toLowerCase())) {
                    return null;
                }
                if ("reload".equalsIgnoreCase(args[0])) {
                    return startsWith(listArg1Reload, args[1]);
                }
            }
        }
        return emptyList;
    }

    public static List<String> startsWith(Collection<String> list, String s) {
        return startsWith(null, list, s);
    }
    public static List<String> startsWith(String[] addition, Collection<String> list, String s) {
        String s1 = s.toLowerCase();
        List<String> stringList = new ArrayList<>(list);
        if (addition != null) stringList.addAll(0, Lists.newArrayList(addition));
        stringList.removeIf(it -> !it.toLowerCase().startsWith(s1));
        return stringList;
    }

    public static String consume(String[] args, int startIndex) {
        StringJoiner joiner = new StringJoiner(" ");
        for (int i = startIndex; i < args.length; i++) {
            joiner.add(args[i]);
        }
        return joiner.toString();
    }
}
