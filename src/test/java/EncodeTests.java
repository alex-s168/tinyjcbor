import dev.vxcc.tinyjcbor.Cbor;
import dev.vxcc.tinyjcbor.serde.*;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HexFormat;
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

    @Test
    public void encodeArgument24() {
        var hex = HexFormat.of();
        // RFC 8949 Appendix A: 24 -> 0x18 0x18
        assertEquals("1818", hex.formatHex(Cbor.encode(ByteOrder.BIG_ENDIAN, 24L, CborPrim.UNSIGNED)));
        // a 24 character string must start with 0x78 0x18
        var s = "abcdefghijklmnopqrstuvwx";
        byte[] b = Cbor.encode(ByteOrder.BIG_ENDIAN, s, CborPrim.STRING);
        assertEquals(26, b.length);
        assertEquals(s, Cbor.decode(ByteBuffer.wrap(b), CborPrim.STRING));
    }

    @Test
    public void encodeArgumentsCanonically() {
        var hex = HexFormat.of();
        // RFC 8949 Appendix A / preferred serialization (section 4.1)
        assertEquals("1864", hex.formatHex(Cbor.encode(ByteOrder.BIG_ENDIAN, 100L, CborPrim.UNSIGNED)));
        assertEquals("18c8", hex.formatHex(Cbor.encode(ByteOrder.BIG_ENDIAN, 200L, CborPrim.UNSIGNED)));
        assertEquals("18ff", hex.formatHex(Cbor.encode(ByteOrder.BIG_ENDIAN, 255L, CborPrim.UNSIGNED)));
        assertEquals("199c40", hex.formatHex(Cbor.encode(ByteOrder.BIG_ENDIAN, 40000L, CborPrim.UNSIGNED)));
        assertEquals("19ffff", hex.formatHex(Cbor.encode(ByteOrder.BIG_ENDIAN, 65535L, CborPrim.UNSIGNED)));
        assertEquals("1affffffff", hex.formatHex(Cbor.encode(ByteOrder.BIG_ENDIAN, 0xFFFFFFFFL, CborPrim.UNSIGNED)));
        assertEquals("3903e7", hex.formatHex(Cbor.encode(ByteOrder.BIG_ENDIAN, -1000L, CborPrim.SIGNED)));
    }

    @Test
    public void sequentialIndefiniteItems() {
        byte[] b = Cbor.encode(ByteOrder.BIG_ENDIAN, null, (e, ignored) -> {
            var t1 = e.writeChunkedText();
            t1.writeChunk("ab");
            t1.end();
            var t2 = e.writeChunkedText();
            t2.writeChunk("cd");
            t2.end();
        });
        // (_ "ab") (_ "cd")
        assertEquals("7f626162ff7f626364ff", HexFormat.of().formatHex(b));
    }

    @Test
    public void indefiniteWriterEndTwiceThrows() {
        assertThrows(IllegalStateException.class, () -> Cbor.encode(ByteOrder.BIG_ENDIAN, null, (e, ignored) -> {
            var t = e.writeChunkedByteString();
            t.end();
            t.end();
        }));
    }

    @Test
    public void nestedIndefiniteContainers() {
        byte[] b = Cbor.encode(ByteOrder.BIG_ENDIAN, null, (e, ignored) -> {
            var outer = e.writeArray();
            var inner = e.writeMap();
            e.writeText("k");
            e.writeUnsigned(1);
            inner.end();
            outer.end();
        });
        // [_ {_ "k": 1}]
        assertEquals("9fbf616b01ffff", HexFormat.of().formatHex(b));
    }
}
