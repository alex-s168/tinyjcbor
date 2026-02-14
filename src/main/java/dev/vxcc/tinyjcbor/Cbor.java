package dev.vxcc.tinyjcbor;

import dev.vxcc.tinyjcbor.serde.CborDeserializer;
import dev.vxcc.tinyjcbor.serde.CborSerializer;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Cbor {
    /** Uses the buffer's byte order as network order for integers & floats */
    public static <T> T decode(@NotNull ByteBuffer buffer, @NotNull CborDeserializer<T> parser) throws InvalidCborException {
        return new CborDecoder(buffer).read(parser);
    }

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
