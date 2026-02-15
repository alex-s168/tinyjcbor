package dev.vxcc.tinyjcbor;

import dev.vxcc.tinyjcbor.serde.CborDeserializer;
import dev.vxcc.tinyjcbor.serde.CborSerializer;
import dev.vxcc.tinyjcbor.util.CborValue;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Cbor {
    /**
     * Reads the CBOR value at the beginning of the buffer
     * <p>Uses the buffer's byte order as network order for integers & floats
     * @since 1.0.0-rc.1
     */
    public static <T> T decode(@NotNull ByteBuffer buffer, @NotNull CborDeserializer<T> parser) throws InvalidCborException {
        return new CborDecoder(buffer).read(parser);
    }

    /**
     * Reads the CBOR value at the beginning of the buffer
     * <p>Uses the buffer's byte order as network order for integers & floats
     * @since 1.0.0-rc.3
     */
    public static CborValue decode(@NotNull ByteBuffer buffer) throws InvalidCborException {
        return new CborDecoder(buffer).read(CborValue.CODEC);
    }

    /**
     * @since 1.0.0-rc.1
     */
    public static <T> byte @NotNull[] encode(@NotNull ByteOrder byteOrder, T value, @NotNull CborSerializer<T> encoder) {
        var out = new ByteArrayOutputStream();
        var e = new CborEncoder(byteOrder, out);
        try {
            encoder.encode(e, value);
            out.flush();
        } catch (IOException ex) { /* how even */
            throw new RuntimeException(ex);
        }
        return out.toByteArray();
    }
}
