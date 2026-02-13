package dev.vxcc.tinyjcbor.util;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Supplier;

public class Collectors {
    public static <T, C extends Collection<T>> Collector<T, C> collection(@NotNull Supplier<C> constructor) {
        return iter -> {
            var out = constructor.get();
            while (iter.hasNext())
                out.add(iter.next());
            return out;
        };
    }

    public static <T> Collector<T, ArrayList<T>> arrayList() {
        return iter -> {
            var out = new ArrayList<T>();
            while (iter.hasNext())
                out.add(iter.next());
            return out;
        };
    }

    public static <T> Collector<T, HashSet<T>> hashSet() {
        return iter -> {
            var out = new HashSet<T>();
            while (iter.hasNext())
                out.add(iter.next());
            return out;
        };
    }

    public static <K, V, M extends Map<K, V>> MapConstructor<K, V, M, M> map(@NotNull Supplier<M> constructor) {
        return new MapConstructor<>() {
            @Override
            public @NotNull M begin() {
                return constructor.get();
            }

            @Override
            public void put(@NotNull M map, K k, V v) {
                map.put(k, v);
            }

            @Override
            public @NotNull M done(@NotNull M map) {
                return map;
            }
        };
    }

    public static <K, V, M extends Map<K, V>> MapConstructor<K, V, M, Map<K, V>> mapDowncast(@NotNull Supplier<M> constructor) {
        return new MapConstructor<>() {
            @Override
            public @NotNull M begin() {
                return constructor.get();
            }

            @Override
            public void put(@NotNull M map, K k, V v) {
                map.put(k, v);
            }

            @Override
            public @NotNull Map<K, V> done(@NotNull M map) {
                return map;
            }
        };
    }
}
