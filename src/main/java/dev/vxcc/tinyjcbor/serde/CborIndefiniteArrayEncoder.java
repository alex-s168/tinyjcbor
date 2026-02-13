package dev.vxcc.tinyjcbor.serde;

import dev.vxcc.tinyjcbor.CborEncoder;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class CborIndefiniteArrayEncoder<T, C extends Iterable<T>> implements CborItemEncoder<C> {
    @NotNull private final CborItemEncoder<T> item;

    public CborIndefiniteArrayEncoder(@NotNull CborItemEncoder<T> item) {
        this.item = item;
    }

    @Override
    public void encode(@NotNull CborEncoder encoder, @NotNull C value) throws IOException {
        var wr = encoder.writeArray();
        for (var x : value) {
            item.encode(encoder, x);
        }
        wr.end();
    }
}
