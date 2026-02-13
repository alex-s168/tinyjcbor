package dev.vxcc.tinyjcbor.serde;

import dev.vxcc.tinyjcbor.CborDecoder;
import dev.vxcc.tinyjcbor.CborType;
import dev.vxcc.tinyjcbor.UnexpectedCborException;
import org.jetbrains.annotations.NotNull;

public abstract class CborTaggedDecoder<I, O> extends CborPrim.PrimitiveDecoder<O> {
    @NotNull private final CborItemDecoder<I> item;

    private static final CborType @NotNull [] ACCEPTS = { CborType.Tagged };

    public CborTaggedDecoder(@NotNull CborItemDecoder<I> item) {
        super(ACCEPTS);
        this.item = item;
    }

    @Override
    public final O next(@NotNull CborDecoder decoder) throws UnexpectedCborException {
        var tag = decoder.getTag();
        var x = item.next(decoder);
        decoder.nextToken();
        return process(tag, x);
    }

    protected abstract O process(long tag, @NotNull I parsed) throws UnexpectedCborException;
}
