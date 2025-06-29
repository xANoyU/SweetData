package top.mrxiaom.sweetdata.commands;
        
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweetdata.Messages;
import top.mrxiaom.sweetdata.SweetData;
import top.mrxiaom.sweetdata.database.PlayerDatabase;
import top.mrxiaom.sweetdata.database.entry.GlobalCache;
import top.mrxiaom.sweetdata.database.entry.PlayerCache;
import top.mrxiaom.sweetdata.func.AbstractModule;

import java.util.*;

@AutoRegister
public class CommandMain extends AbstractModule implements CommandExecutor, TabCompleter, Listener {
    private final boolean bungeecord;
    public CommandMain(SweetData plugin) {
        super(plugin);
        registerCommand("sweetdata", this);
        bungeecord = Bukkit.spigot().getConfig().getBoolean("settings.bungeecord", false);
    }

    boolean consoleSilentPlus;
    boolean unsafeMode;

    @Override
    public void reloadConfig(MemoryConfiguration config) {
        consoleSilentPlus = config.getBoolean("console-silent-plus", true);
        unsafeMode = config.getBoolean("unsafe-mode", false);
    }

    @SuppressWarnings("SameParameterValue")
    private boolean check(String[] command, String permission, String arg, CommandSender sender) {
        for (String s : command) {
            if (arg.equalsIgnoreCase(s)) {
                return sender.hasPermission(permission);
            }
        }
        return false;
    }
    private boolean check(String command, String permission, String arg, CommandSender sender) {
        return arg.equalsIgnoreCase(command) && sender.hasPermission(permission);
    }

    private final String[] commandRemoveDel = {"remove", "del"};
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length > 1 && ("global".equalsIgnoreCase(args[0]) || "g".equalsIgnoreCase(args[0]))) {
            if (globalCommand(sender, args)) {
                return true;
            }
        }
        if (args.length >= 3 && check("get", "sweet.data.player.get", args[0], sender)) {
            OfflinePlayer player = Util.getOfflinePlayer(args[1]).orElse(null);
            if (player == null) {
                return Messages.command__player_not_found.tm(sender,
                        Pair.of("%player%", args[1]));
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
                return Messages.command__get__success.tm(sender,
                        Pair.of("%player%", args[1]),
                        Pair.of("%key%", key),
                        Pair.of("%value%", value));
            } else {
                return Messages.command__get__not_found.tm(sender,
                        Pair.of("%player%", args[1]),
                        Pair.of("%key%", key));
            }
        }
        if (args.length >= 4 && check("set", "sweet.data.player.set", args[0], sender)) {
            OfflinePlayer player = Util.getOfflinePlayer(args[1]).orElse(null);
            if (player == null) {
                Messages.command__player_not_found.tm(sender,
                        Pair.of("%player%", args[1]));
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
            return Messages.command__set__success.tm(sender,
                    Pair.of("%player%", args[1]),
                    Pair.of("%key%", key),
                    Pair.of("%value%", value));
        }
        if (args.length >= 3 && check(commandRemoveDel, "sweet.data.player.del", args[0], sender)) {
            OfflinePlayer player = Util.getOfflinePlayer(args[1]).orElse(null);
            if (player == null) {
                return Messages.command__player_not_found.tm(sender,
                        Pair.of("%player%", args[1]));
            }
            String key = args[2];

            PlayerDatabase db = plugin.getPlayerDatabase();
            PlayerCache cache = db.getCacheOrNull(player);
            if (cache != null) {
                cache.remove(key);
            }
            db.remove(player, key);
            return Messages.command__remove__success.tm(sender,
                    Pair.of("%player%", args[1]),
                    Pair.of("%key%", key));
        }
        if (args.length >= 4 && check("plus", "sweet.data.player.plus", args[0], sender)) {
            OfflinePlayer player = Util.getOfflinePlayer(args[1]).orElse(null);
            if (player == null) {
                return Messages.command__player_not_found.tm(sender,
                        Pair.of("%player%", args[1]));
            }
            String key = args[2];
            Integer toAdd = Util.parseInt(args[3]).orElse(null);
            if (toAdd == null) {
                return Messages.command__plus__not_integer.tm(sender,
                        Pair.of("%input%", args[3]));
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
            if (consoleSilentPlus && sender instanceof ConsoleCommandSender) {
                return true;
            }
            if (result != null) {
                return Messages.command__plus__success.tm(sender,
                        Pair.of("%player%", args[1]),
                        Pair.of("%key%", key),
                        Pair.of("%value%", result));
            }
            return Messages.command__plus__fail.tm(sender,
                    Pair.of("%player%", args[1]),
                    Pair.of("%key%", key),
                    Pair.of("%added%", toAdd));
        }
        if (args.length > 0 && check("reload", "sweet.data.reload", args[0], sender)) {
            if (args.length > 1 && "database".equalsIgnoreCase(args[1])) {
                plugin.options.database().reloadConfig();
                plugin.options.database().reconnect();
                PlayerDatabase db = plugin.getPlayerDatabase();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    db.refreshCache(player);
                }
                return Messages.command__reload__database.tm(sender);
            }
            plugin.reloadConfig();
            return Messages.command__reload__normal.tm(sender);
        }
        if (sender.hasPermission("sweet.data.help")) {
            return Messages.command__help.tm(sender);
        }
        return true;
    }
    private boolean globalCommand(@NotNull CommandSender sender, String[] args) {
        if (check("get", "sweet.data.global.get", args[1], sender)) {
            String key = args[2];
            String value;

            PlayerDatabase db = plugin.getPlayerDatabase();
            GlobalCache global = db.getGlobalCache();
            value = global.get(key).orElse(null);

            if (value != null) {
                return Messages.command__global__get__success.tm(sender,
                        Pair.of("%key%", key),
                        Pair.of("%value%", value));
            } else {
                return Messages.command__global__get__not_found.tm(sender,
                        Pair.of("%key%", key));
            }
        }
        if (check("set", "sweet.data.global.set", args[1], sender)) {
            String key = args[2];
            String value = consume(args, 3);
            Player whoever;
            if (bungeecord) {
                whoever = Iterables.getFirst(Bukkit.getOnlinePlayers(), null);
                if (whoever == null && !unsafeMode) {
                    return Messages.command__global__unsafe.tm(sender);
                }
            } else {
                whoever = null;
            }

            PlayerDatabase db = plugin.getPlayerDatabase();
            db.globalSet(key, value);
            db.sendRequireGlobalCacheUpdate(whoever, key, value);

            return Messages.command__global__set__success.tm(sender,
                    Pair.of("%key%", key),
                    Pair.of("%value%", value));
        }
        if (check(commandRemoveDel, "sweet.data.global.del", args[1], sender)) {
            String key = args[2];
            Player whoever;
            if (bungeecord) {
                whoever = Iterables.getFirst(Bukkit.getOnlinePlayers(), null);
                if (whoever == null && !unsafeMode) {
                    return Messages.command__global__unsafe.tm(sender);
                }
            } else {
                whoever = null;
            }

            PlayerDatabase db = plugin.getPlayerDatabase();
            db.globalRemove(key);
            db.sendRequireGlobalCacheUpdate(whoever, key, null);

            return Messages.command__global__remove__success.tm(sender,
                    Pair.of("%key%", key));
        }
        if (check("plus", "sweet.data.global.plus", args[1], sender)) {
            String key = args[2];
            Integer toAdd = Util.parseInt(args[3]).orElse(null);
            if (toAdd == null) {
                return Messages.command__global__plus__not_integer.tm(sender,
                        Pair.of("%input%", args[3]));
            }
            Player whoever;
            if (bungeecord) {
                whoever = Iterables.getFirst(Bukkit.getOnlinePlayers(), null);
                if (whoever == null && !unsafeMode) {
                    return Messages.command__global__unsafe.tm(sender);
                }
            } else {
                whoever = null;
            }
            PlayerDatabase db = plugin.getPlayerDatabase();
            Integer result = db.globalIntAdd(key, toAdd);
            if (result != null) {
                db.sendRequireGlobalCacheUpdate(whoever, key, String.valueOf(result));
            }

            if (consoleSilentPlus && sender instanceof ConsoleCommandSender) {
                return true;
            }
            if (result != null) {
                return Messages.command__global__plus__success.tm(sender,
                        Pair.of("%added%", toAdd),
                        Pair.of("%key%", key),
                        Pair.of("%value%", result));
            }
            return Messages.command__global__plus__fail.tm(sender,
                    Pair.of("%key%", key),
                    Pair.of("%added%", toAdd));
        }
        return false;
    }

    private void tab(CommandSender sender, List<String> list, String permission, String... args) {
        if (sender.hasPermission(permission)) {
            list.addAll(Arrays.asList(args));
        }
    }
    private static final List<String> emptyList = Collections.emptyList();
    private static final List<String> listArg1Player = Lists.newArrayList(
            "get", "set", "plus", "remove", "del");
    private static final List<String> listArg1Reload = Lists.newArrayList(
            "database");
    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            tab(sender, list, "sweet.data.global", "global", "g");
            tab(sender, list, "sweet.data.player.get", "get");
            tab(sender, list, "sweet.data.player.set", "set");
            tab(sender, list, "sweet.data.player.del", "remove", "del");
            tab(sender, list, "sweet.data.player.plus", "plus");
            tab(sender, list, "sweet.data.reload", "reload");
            return startsWith(list, args[0]);
        }
        if (args.length == 2) {
            if (listArg1Player.contains(args[0].toLowerCase())) {
                String permKey = (args[0].equalsIgnoreCase("remove") ? "del" : args[0]).toLowerCase();
                if (sender.hasPermission(permKey)) {
                    return null;
                }
            }
            if ("global".equalsIgnoreCase(args[0]) || "g".equalsIgnoreCase(args[0])) {
                List<String> list = new ArrayList<>();
                tab(sender, list, "sweet.data.global.get", "get");
                tab(sender, list, "sweet.data.global.set", "set");
                tab(sender, list, "sweet.data.global.del", "remove", "del");
                tab(sender, list, "sweet.data.global.plus", "plus");
                return startsWith(list, args[1]);
            }
            if (check("reload", "sweet.data.reload", args[0], sender)) {
                return startsWith(listArg1Reload, args[1]);
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
