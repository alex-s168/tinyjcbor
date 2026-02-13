package dev.vxcc.tinyjcbor.serde;

import dev.vxcc.tinyjcbor.CborEncoder;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Map;

public class CborMapEncoder<K, V, M extends Map<K, V>> implements CborItemEncoder<M> {
    @NotNull
    private final CborItemEncoder<K> key;
    @NotNull
    private final CborItemEncoder<V> val;

    public CborMapEncoder(@NotNull CborItemEncoder<K> key, @NotNull CborItemEncoder<V> val) {
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
