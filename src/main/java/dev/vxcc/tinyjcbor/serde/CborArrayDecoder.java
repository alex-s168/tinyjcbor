package dev.vxcc.tinyjcbor.serde;

import dev.vxcc.tinyjcbor.CborDecoder;
import dev.vxcc.tinyjcbor.CborType;
import dev.vxcc.tinyjcbor.util.Collector;
import dev.vxcc.tinyjcbor.UnexpectedCborException;
import org.jetbrains.annotations.NotNull;

public final class CborArrayDecoder<I, T> extends CborPrim.PrimitiveDecoder<T> {

    @NotNull private final Collector<I, T> collector;
    @NotNull private final CborItemDecoder<I> item;

    private static final CborType @NotNull [] ACCEPTS = { CborType.Array };

    public CborArrayDecoder(@NotNull Collector<I, T> collector, @NotNull CborItemDecoder<I> item) {
        super(ACCEPTS);
        this.collector = collector;
        this.item = item;
    }

    @Override
    public T next(@NotNull CborDecoder decoder) throws UnexpectedCborException {
        return collector.collect(decoder.readArray(item));
    }
}
