package dev.vxcc.tinyjcbor.serde;

import dev.vxcc.tinyjcbor.CborDecoder;
import dev.vxcc.tinyjcbor.CborType;
import dev.vxcc.tinyjcbor.UnexpectedCborException;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Collector;

public final class CborArrayDecoder<T, R> extends CborPrim.PrimitiveDecoder<R> {
    @NotNull private final Collector<T, ?, R> collector;
    @NotNull private final CborDeserializer<T> item;

    private static final CborType @NotNull [] ACCEPTS = { CborType.Array };

    public CborArrayDecoder(@NotNull Collector<T, ?, R> collector, @NotNull CborDeserializer<T> item) {
        super(ACCEPTS);
        this.collector = collector;
        this.item = item;
    }

    @Override
    public R next(@NotNull CborDecoder decoder) throws UnexpectedCborException {
        return next(collector, item, decoder);
    }

    private static <T, A, R> R next(@NotNull Collector<T, A, R> collector, @NotNull CborDeserializer<T> item, @NotNull CborDecoder decoder) {
        var out = collector.supplier().get();
        var iter = decoder.readArray(item);
        while (iter.hasNext()) {
            var v = iter.next();
            collector.accumulator().accept(out, v);
        }
        return collector.finisher().apply(out);
    }
}
