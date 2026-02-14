package dev.vxcc.tinyjcbor.serde;

import dev.vxcc.tinyjcbor.CborDecoder;
import dev.vxcc.tinyjcbor.CborType;
import dev.vxcc.tinyjcbor.UnexpectedCborException;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class CborByteArrayDecoder<R> extends CborPrim.PrimitiveDecoder<R> {
    @NotNull private final Function<CborDecoder.ByteReader, R> consumer;

    private static final CborType @NotNull [] ACCEPTS = { CborType.ByteString};

    public CborByteArrayDecoder(@NotNull Function<CborDecoder.ByteReader, R> consumer) {
        super(ACCEPTS);
        this.consumer = consumer;
    }

    @Override
    public R next(@NotNull CborDecoder decoder) throws UnexpectedCborException {
        var by = decoder.readByteString();
        var res = consumer.apply(by);
        by.skipToEnd();
        return res;
    }
}
