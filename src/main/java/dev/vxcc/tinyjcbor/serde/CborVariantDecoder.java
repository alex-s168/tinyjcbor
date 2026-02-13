package dev.vxcc.tinyjcbor.serde;

import dev.vxcc.tinyjcbor.CborDecoder;
import dev.vxcc.tinyjcbor.CborType;
import dev.vxcc.tinyjcbor.UnexpectedCborException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class CborVariantDecoder<T> implements CborItemDecoder<T> {
    @NotNull private final Object[] decoders;
    @NotNull private final Map<CborType, CborItemDecoder<?>[]> byType;
    @NotNull private final Set<CborType> neverAccepts;
    @NotNull private final CborDecoder.Snapshot snapshot = new CborDecoder.Snapshot();
    @Nullable private String expected;

    public CborVariantDecoder(@NotNull List<CborItemDecoder<? extends T>> decodersIn) {
        var decoders = new ArrayList<CborItemDecoder<? extends T>>();
        for (var decoder : decodersIn) {
            if (decoder instanceof CborVariantDecoder<? extends T> v) {
                for (var x : v.decoders) {
                    decoders.add((CborItemDecoder<? extends T>) x);
                }
            } else {
                decoders.add(decoder);
            }
        }

        var byType = new HashMap<CborType, CborItemDecoder<?>[]>();
        var neverAccepts = new HashSet<CborType>();
        for (var type : CborType.values()) {
            var anyNotNeverAccepts = false;

            var possible = new ArrayList<CborItemDecoder<?>>();
            for (var decoder : decoders) {
                if (decoder.mightAccept(type))
                    possible.add(decoder);
                if (!decoder.neverAccepts(type))
                    anyNotNeverAccepts = true;
            }
            var arr = new CborItemDecoder[possible.size()];
            possible.toArray(arr);
            byType.put(type, arr);

            if (!anyNotNeverAccepts)
                neverAccepts.add(type);
        }
        this.byType = byType;
        this.neverAccepts = neverAccepts;
        this.decoders = decoders.toArray();
    }

    public void setExpected(@Nullable String expected) {
        this.expected = expected;
    }

    @Override
    public T next(@NotNull CborDecoder decoder) throws UnexpectedCborException {
        var type = decoder.tokenType();
        var possible = byType.get(type);
        if (possible != null) {
            snapshot.from(decoder);
            for (var child : possible) {
                try {
                    return ((CborItemDecoder<? extends T>) child).next(decoder);
                } catch (UnexpectedCborException ignored) {
                    decoder.reset(snapshot);
                }
            }
        }
        throw new UnexpectedCborException.UnexpectedType(expected, type);
    }

    @Override
    public boolean mightAccept(@NotNull CborType type) {
        return byType.containsKey(type);
    }

    @Override
    public boolean neverAccepts(@NotNull CborType type) {
        return neverAccepts.contains(type);
    }
}
