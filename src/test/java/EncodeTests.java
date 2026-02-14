import dev.vxcc.tinyjcbor.Cbor;
import dev.vxcc.tinyjcbor.serde.*;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class EncodeTests {
    @Test
    public void taggedIntArray() {
        var buf = ByteBuffer.wrap(Cbor.encode(
            ByteOrder.BIG_ENDIAN,
            List.of(-2L, 27315L),
            new CborFixedTagEncoder<>(4,
                new CborCollectionArrayEncoder<>(CborPrim.SIGNED))
        ));

        assertEquals((byte) 0xc4, buf.get());
        assertEquals((byte) 0x82, buf.get());
        assertEquals((byte) 0x21, buf.get());
        assertEquals((byte) 0x19, buf.get());
        assertEquals((short) 0x6ab3, buf.getShort());
        assertFalse(buf.hasRemaining());
    }

    @Test
    public void indefiniteLengthByteString() {
        var buf = ByteBuffer.wrap(Cbor.encode(
            ByteOrder.BIG_ENDIAN,
            List.of(new byte[]{
                    (byte) 0xaa,
                    (byte) 0xbb,
                    (byte) 0xcc,
                    (byte) 0xdd,
            },
            new byte[]{
                (byte) 0xee,
                (byte) 0xff,
                (byte) 0x99,
            }),
            (out, x) -> {
                var cw = out.writeChunkedByteString();
                for (var chunk : x) {
                    cw.writeChunk(chunk);
                }
                cw.end();
            }
        ));

        assertEquals((byte) 0x5f, buf.get());
        assertEquals((byte) 0x44, buf.get());
        assertEquals((byte) 0xaa, buf.get());
        assertEquals((byte) 0xbb, buf.get());
        assertEquals((byte) 0xcc, buf.get());
        assertEquals((byte) 0xdd, buf.get());
        assertEquals((byte) 0x43, buf.get());
        assertEquals((byte) 0xee, buf.get());
        assertEquals((byte) 0xff, buf.get());
        assertEquals((byte) 0x99, buf.get());
        assertEquals((byte) 0xff, buf.get());
        assertFalse(buf.hasRemaining());
    }
}
