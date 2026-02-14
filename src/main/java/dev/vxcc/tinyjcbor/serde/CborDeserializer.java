package dev.vxcc.tinyjcbor.serde;

import dev.vxcc.tinyjcbor.CborDecoder;
import dev.vxcc.tinyjcbor.CborType;
import dev.vxcc.tinyjcbor.UnexpectedCborException;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;
import java.util.function.Function;

@FunctionalInterface
public interface CborDeserializer<T> {
    T next(@NotNull CborDecoder decoder) throws UnexpectedCborException;

    default boolean mightAccept(@NotNull CborType type) {
        return true;
    }

    default boolean neverAccepts(@NotNull CborType type) {
        return false;
    }

    static <T, R> @NotNull CborDeserializer<R> map(@NotNull CborDeserializer<T> parent, @NotNull Function<T, R> fn) {
        return new CborDeserializer<>() {
            @Override
            public R next(@NotNull CborDecoder decoder) throws UnexpectedCborException {
                return fn.apply(parent.next(decoder));
            }

            @Override
            public boolean mightAccept(@NotNull CborType type) {
                return parent.mightAccept(type);
            }

            @Override
            public boolean neverAccepts(@NotNull CborType type) {
                return parent.neverAccepts(type);
            }
        };
    }

    static <T, B, R> @NotNull CborDeserializer<R> chain(@NotNull CborDeserializer<T> parent, @NotNull CborDeserializer<B> other, @NotNull BiFunction<T, B, R> then) {
        return new CborDeserializer<>() {
            @Override
            public R next(@NotNull CborDecoder decoder) throws UnexpectedCborException {
                var a = parent.next(decoder);
                var b = other.next(decoder);
                return then.apply(a, b);
            }

            @Override
            public boolean mightAccept(@NotNull CborType type) {
                return parent.mightAccept(type);
            }

            @Override
            public boolean neverAccepts(@NotNull CborType type) {
                return parent.neverAccepts(type);
            }
        };
    }
}