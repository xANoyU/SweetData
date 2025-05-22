package top.mrxiaom.sweetdata.database.entry;

import org.bukkit.entity.Player;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.pluginbase.utils.Util;

import java.util.*;

public class PlayerCache {
    private final Player player;
    private final Map<String, String> data = new TreeMap<>();
    private final Set<String> modifiedKeys = new HashSet<>();
    private long nextSubmitTime = 0L;
    public PlayerCache(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    public boolean shouldSubmitCache() {
        if (nextSubmitTime == 0L) return false;
        if (System.currentTimeMillis() > nextSubmitTime) {
            nextSubmitTime = 0L;
            return true;
        }
        return false;
    }

    public long getNextSubmitTime() {
        return nextSubmitTime;
    }

    public void setNextSubmitAfter(long timeMills, boolean cover) {
        if (!cover && nextSubmitTime != 0L) return;
        nextSubmitTime = System.currentTimeMillis() + timeMills;
    }

    public void put(String key, String value) {
        data.put(key, value);
        modifiedKeys.add(key);
    }

    public void put(String key, int value) {
        data.put(key, String.valueOf(value));
        modifiedKeys.add(key);
    }

    public void remove(String key) {
        data.remove(key);
        modifiedKeys.remove(key);
    }

    public Optional<String> get(String key) {
        return Optional.ofNullable(data.get(key));
    }

    public Optional<Integer> getInt(String key) {
        return Optional.ofNullable(data.get(key)).flatMap(Util::parseInt);
    }

    public void putAll(List<Pair<String, String>> pairs) {
        for (Pair<String, String> pair : pairs) {
            data.put(pair.key(), pair.value());
        }
    }

    public List<Pair<String, String>> getToSubmitPairs() {
        List<Pair<String, String>> list = new ArrayList<>();
        for (String modifiedKey : modifiedKeys) {
            String value = data.get(modifiedKey);
            if (value != null) {
                list.add(Pair.of(modifiedKey, value));
            }
        }
        modifiedKeys.clear();
        return list;
    }
}
