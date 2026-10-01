package name.blockrooms.util;

import java.util.HashMap;
import java.util.Map;

public class FlexibleMap<T, U, V> {
    private final Map<T, V> single = new HashMap<>();
    private final Map<T, Map<U, V>> multi = new HashMap<>();

    public void put(T t, V v) {
        single.putIfAbsent(t, v);
    }
    public void put(T t, U u, V v) {
        multi.computeIfAbsent(t, k -> new HashMap<>())
                .putIfAbsent(u, v);
    }
    public V get(T t, U u) {
        if (multi.containsKey(t) && multi.get(t).containsKey(u)) {
            return multi.get(t).get(u);
        } else return single.getOrDefault(t, null);
    }
}
