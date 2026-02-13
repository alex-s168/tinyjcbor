package dev.vxcc.tinyjcbor.serde;

import dev.vxcc.tinyjcbor.CborDecoder;
import dev.vxcc.tinyjcbor.CborType;
import dev.vxcc.tinyjcbor.UnexpectedCborException;
import org.jetbrains.annotations.NotNull;

public final class CborFixedTagDecoder<T> extends CborPrim.PrimitiveDecoder<T> {
    @NotNull private final CborItemDecoder<T> item;
    private final long tag;

    private static final CborType @NotNull [] ACCEPTS = { CborType.Tagged };

    public CborFixedTagDecoder(long tag, @NotNull CborItemDecoder<T> item) {
        super(ACCEPTS);
        this.item = item;
        this.tag = tag;
    }

    @Override
    public T next(@NotNull CborDecoder decoder) throws UnexpectedCborException {
        var x = decoder.readTag();
        if (x != tag)
            throw new UnexpectedCborException.WrongTag(tag, x);
        return item.next(decoder);
    }
}
