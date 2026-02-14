package dev.vxcc.tinyjcbor.serde;

import dev.vxcc.tinyjcbor.CborEncoder;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Map;

public class CborMapEncoder<K, V, M extends Map<K, V>> implements CborSerializer<M> {
    @NotNull
    private final CborSerializer<K> key;
    @NotNull
    private final CborSerializer<V> val;

    public CborMapEncoder(@NotNull CborSerializer<K> key, @NotNull CborSerializer<V> val) {
        this.key = key;
        this.val = val;
    }

    @Override
    public void encode(@NotNull CborEncoder encoder, @NotNull M value) throws IOException {
        encoder.writeMap(value.size());
        for (var entry : value.entrySet()) {
            key.encode(encoder, entry.getKey());
            val.encode(encoder, entry.getValue());
        }
    }
}
