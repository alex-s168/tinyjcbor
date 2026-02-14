package dev.vxcc.tinyjcbor.serde;

import dev.vxcc.tinyjcbor.CborEncoder;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.function.Function;

@FunctionalInterface
public interface CborSerializer<T> {
    void encode(@NotNull CborEncoder encoder, T value) throws IOException;

    static <I, T> CborSerializer<I> map(@NotNull CborSerializer<T> original, @NotNull Function<I, T> fn) {
        return (encoder, value) ->
                original.encode(encoder, fn.apply(value));
    }
}
