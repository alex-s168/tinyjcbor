package dev.vxcc.tinyjcbor;

import dev.vxcc.tinyjcbor.serde.CborDeserializer;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.NoSuchElementException;

public class CborSeq {
    @NotNull private final CborDecoder decoder;
    @NotNull private final CborDecoder.Snapshot snapshot = new CborDecoder.Snapshot();

    public CborSeq(@NotNull ByteBuffer buffer) {
        this.decoder = new CborDecoder(buffer);
    }

    public boolean hasNext() {
        return decoder.hasNext();
    }

    public <T> @NotNull T next(@NotNull CborDeserializer<T> item) throws UnexpectedCborException {
        if (!decoder.hasNext())
            throw new NoSuchElementException();
        snapshot.from(decoder);
        T parsed;
        try {
            parsed = item.next(decoder);
        } catch (Throwable e) {
            decoder.reset(snapshot);
            throw e;
        }
        return parsed;
    }
}
