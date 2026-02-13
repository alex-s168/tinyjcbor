package dev.vxcc.tinyjcbor.serde;

import dev.vxcc.tinyjcbor.CborDecoder;
import dev.vxcc.tinyjcbor.CborType;
import dev.vxcc.tinyjcbor.UnexpectedCborException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

public abstract class CborStringDecoder<R> extends CborPrim.PrimitiveDecoder<R> {

    private static final CborType @NotNull [] ACCEPTS = { CborType.Utf8String };

    public CborStringDecoder() {
        super(ACCEPTS);
    }

    @Override
    public final R next(@NotNull CborDecoder decoder) throws UnexpectedCborException {
        var reader = decoder.getUtf8();
        R res;
        try {
            res = process(reader);
            try {
                reader.transferTo(Writer.nullWriter());
            } catch (IOException ignored) {}
        } finally {
            try {
                reader.close();
            } catch (IOException ignored) {}
        }
        return res;
    }

    abstract R process(Reader r);
}
