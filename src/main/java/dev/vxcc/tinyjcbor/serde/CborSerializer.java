package dev.vxcc.tinyjcbor.serde;

import dev.vxcc.tinyjcbor.CborEncoder;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.function.Function;

/**
 * A functional interface for serializing a {@code T} into an {@code CborEncoder}
 *
 * @param <T> The type it can serialize
 *
 * @since 1.0.0-rc.1
 */
@FunctionalInterface
public interface CborSerializer<T> {
    /**
     * Serializes the given value into the CborEncoder
     *
     * @throws IOException When the {@code CborEncoder} failed to write to the output
     *
     * @since 1.0.0-rc.1
     */
    void encode(@NotNull CborEncoder encoder, T value) throws IOException;

    /**
     * Apply a function to a value before serializing it.
     *
     * @since 1.0.0-rc.1
     */
    static <I, T> CborSerializer<I> map(@NotNull CborSerializer<T> original, @NotNull Function<I, T> fn) {
        return (encoder, value) ->
                original.encode(encoder, fn.apply(value));
    }
}
