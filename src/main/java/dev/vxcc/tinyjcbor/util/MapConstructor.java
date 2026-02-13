package dev.vxcc.tinyjcbor.util;

import org.jetbrains.annotations.NotNull;

public interface MapConstructor<K, V, Impl, O> {
    @NotNull Impl begin();
    void put(@NotNull Impl map, K k, V v);
    @NotNull O done(@NotNull Impl map);
}
