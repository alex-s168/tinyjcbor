package dev.vxcc.tinyjcbor;

import dev.vxcc.tinyjcbor.serde.CborItemDecoder;
import dev.vxcc.tinyjcbor.serde.CborItemEncoder;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Cbor {
    /** Uses the buffer's byte order as network order for integers & floats */
    public static <T> T decode(@NotNull ByteBuffer buffer, @NotNull CborItemDecoder<T> parser) throws InvalidCborException {
        var decoder = new CborDecoder(buffer);
        decoder.nextToken();
        return parser.next(decoder);
    }

    public static <T> byte @NotNull[] encode(@NotNull ByteOrder byteOrder, T value, @NotNull CborItemEncoder<T> encoder) {
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
