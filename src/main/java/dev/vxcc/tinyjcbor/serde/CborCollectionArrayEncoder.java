package dev.vxcc.tinyjcbor.serde;

import dev.vxcc.tinyjcbor.CborEncoder;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Collection;

public class CborCollectionArrayEncoder<T, C extends Collection<T>> implements CborSerializer<C> {
    @NotNull
    private final CborSerializer<T> item;

    public CborCollectionArrayEncoder(@NotNull CborSerializer<T> item) {
        this.item = item;
    }

    @Override
    public void encode(@NotNull CborEncoder encoder, @NotNull C value) throws IOException {
        encoder.writeArray(value.size());
        for (var x : value) {
            item.encode(encoder, x);
        }
    }
}
