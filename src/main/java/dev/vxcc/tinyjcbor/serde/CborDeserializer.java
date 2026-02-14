package dev.vxcc.tinyjcbor.serde;

import dev.vxcc.tinyjcbor.CborDecoder;
import dev.vxcc.tinyjcbor.CborType;
import dev.vxcc.tinyjcbor.UnexpectedCborException;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A functional interface for deserializing a {@code T} from an {@code CborDecoder}
 *
 * @param <T> The type it deserializes to
 *
 * @since 1.0.0-rc.1
 */
@FunctionalInterface
public interface CborDeserializer<T> {
    /**
     * Try to deserialize a {@code T} from a CBOR buffer
     *
     * @param decoder The CBOR decoder
     * @return The deserialized object
     * @throws UnexpectedCborException If the data in the CBOR buffer does not match the expected schema
     *
     * @since 1.0.0-rc.1
     */
    T next(@NotNull CborDecoder decoder) throws UnexpectedCborException;

    /**
     * @return true, if there is a possibility for the deserializer accepting an item with the given type
     *
     * @since 1.0.0-rc.1
     */
    default boolean mightAccept(@NotNull CborType type) {
        return true;
    }

    /**
     *
     * @return true, if there is no possibility at all for the deserializer accepting an item with the given type
     *
     * @since 1.0.0-rc.1
     */
    default boolean neverAccepts(@NotNull CborType type) {
        return false;
    }

    /**
     * Apply a function to the deserialized item
     * @since 1.0.0-rc.1
     */
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

    /**
     * First execute this decoder, then the other decoder, and then finally combine the two results with a function, returning the result
     *
     * @since 1.0.0-rc.1
     */
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