package dev.vxcc.tinyjcbor.serde;

import dev.vxcc.tinyjcbor.CborDecoder;
import dev.vxcc.tinyjcbor.CborEncoder;
import dev.vxcc.tinyjcbor.CborType;
import dev.vxcc.tinyjcbor.UnexpectedCborException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class CborSerDe<T> implements CborItemDecoder<T>, CborItemEncoder<T> {
    @NotNull public final CborItemEncoder<T> encoder;
    @NotNull public final CborItemDecoder<T> decoder;

    public CborSerDe(@NotNull CborItemDecoder<T> decoder, @NotNull CborItemEncoder<T> encoder) {
        this.encoder = encoder;
        this.decoder = decoder;
    }


    @Override
    public T next(@NotNull CborDecoder in) throws UnexpectedCborException {
        return decoder.next(in);
    }

    @Override
    public boolean mightAccept(@NotNull CborType type) {
        return decoder.mightAccept(type);
    }

    @Override
    public boolean neverAccepts(@NotNull CborType type) {
        return decoder.neverAccepts(type);
    }

    @Override
    public void encode(@NotNull CborEncoder out, T value) throws IOException {
        encoder.encode(out, value);
    }
}
