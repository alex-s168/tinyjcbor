package dev.vxcc.tinyjcbor.serde;

import dev.vxcc.tinyjcbor.CborDecoder;
import dev.vxcc.tinyjcbor.CborType;
import dev.vxcc.tinyjcbor.UnexpectedCborException;
import dev.vxcc.tinyjcbor.util.MapConstructor;
import org.jetbrains.annotations.NotNull;

public class CborMapDecoder<K, V, O> implements CborItemDecoder<O> {
    @NotNull private final Impl<K, V, ?, O> impl;

    public CborMapDecoder(@NotNull MapConstructor<K, V, ?, O> constructor, @NotNull CborItemDecoder<K> key, @NotNull CborItemDecoder<V> val) {
        this.impl = new Impl(constructor, key, val);
    }

    @Override
    public O next(@NotNull CborDecoder decoder) throws UnexpectedCborException {
        return impl.next(decoder);
    }

    @Override
    public boolean mightAccept(@NotNull CborType type) {
        return impl.mightAccept(type);
    }

    @Override
    public boolean neverAccepts(@NotNull CborType type) {
        return impl.neverAccepts(type);
    }

    private static class Impl<K, V, Impl, O> extends CborPrim.PrimitiveDecoder<O> {
        @NotNull
        private final MapConstructor<K, V, Impl, O> constructor;
        @NotNull private final CborItemDecoder<K> key;
        @NotNull private final CborItemDecoder<V> val;

        private static final CborType @NotNull [] ACCEPTS = { CborType.Map };

        public Impl(@NotNull MapConstructor<K, V, Impl, O> constructor, @NotNull CborItemDecoder<K> key, @NotNull CborItemDecoder<V> val) {
            super(ACCEPTS);
            this.constructor = constructor;
            this.key = key;
            this.val = val;
        }

        @Override
        public final @NotNull O next(@NotNull CborDecoder decoder) throws UnexpectedCborException {
            var map = constructor.begin();
            decoder.readMap(key, val, (k, v) -> {
                constructor.put(map, k, v);
            });
            return constructor.done(map);
        }
    }
}

