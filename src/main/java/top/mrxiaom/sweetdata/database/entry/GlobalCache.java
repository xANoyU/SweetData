package top.mrxiaom.sweetdata.database.entry;

import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.pluginbase.utils.Util;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public class GlobalCache {
    private final Map<String, String> data = new TreeMap<>();
    public GlobalCache() {
    }

    public void put(String key, String value) {
        data.put(key, value);
    }

    public void put(String key, int value) {
        data.put(key, String.valueOf(value));
    }

    public void remove(String key) {
        data.remove(key);
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
}
