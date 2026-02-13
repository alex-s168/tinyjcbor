package dev.vxcc.tinyjcbor.serde;

import dev.vxcc.tinyjcbor.CborEncoder;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class CborFixedTagEncoder<T> implements CborItemEncoder<T> {
    private final long tag;
    @NotNull private final CborItemEncoder<T> item;

    public CborFixedTagEncoder(long tag, @NotNull CborItemEncoder<T> item) {
        this.tag = tag;
        this.item = item;
    }

    @Override
    public void encode(@NotNull CborEncoder encoder, @NotNull T value) throws IOException {
        encoder.writeTag(tag);
        item.encode(encoder, value);
    }
}
