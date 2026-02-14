package dev.vxcc.tinyjcbor.util;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Supplier;

public interface MapConstructor<K, V, Impl, O> {
    @NotNull Impl begin();

    void put(@NotNull Impl map, K k, V v);

    @NotNull O done(@NotNull Impl map);

    static <K, V, M extends Map<K, V>> MapConstructor<K, V, M, M> map(@NotNull Supplier<M> constructor) {
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

    static <K, V, M extends Map<K, V>> MapConstructor<K, V, M, Map<K, V>> mapDowncast(@NotNull Supplier<M> constructor) {
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
