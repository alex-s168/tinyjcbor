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

    // TODO
    /*
    @Test
    public void nestedFiniteLengthArraysVariant() {
        var buf = ByteBuffer.allocate(64);
        buf.put((byte) 0x83);
        buf.put((byte) 0x01);
        buf.put((byte) 0x82);
        buf.put((byte) 0x02);
        buf.put((byte) 0x03);
        buf.put((byte) 0x82);
        buf.put((byte) 0x04);
        buf.put((byte) 0x05);
        buf.flip();

        var item = new CborArrayDecoder<Object, ArrayList<Object>>(Collectors.arrayList(), new CborVariantDecoder<>(List.of(
                CborPrim.UNSIGNED,
                new CborArrayDecoder<>(Collectors.arrayList(), CborPrim.UNSIGNED)
        )));
        var x = Cbor.decode(buf, item);
        assertEquals(3, x.size());
        assertEquals(1, (long) (Long) x.get(0));
        var arr1 = assertInstanceOf(ArrayList.class, x.get(1));
        var arr2 = assertInstanceOf(ArrayList.class, x.get(2));
        assertEquals(2, arr1.size());
        assertEquals(2, (long) (Long) arr1.get(0));
        assertEquals(3, (long) (Long) arr1.get(1));
        assertEquals(2, arr2.size());
        assertEquals(4, (long) (Long) arr2.get(0));
        assertEquals(5, (long) (Long) arr2.get(1));
    }

    @Test
    public void nestedMixedIndefiniteArraysVariant() {
        var buf = ByteBuffer.allocate(64);
        buf.put((byte) 0x9f);
        buf.put((byte) 0x01);
        buf.put((byte) 0x82);
        buf.put((byte) 0x02);
        buf.put((byte) 0x03);
        buf.put((byte) 0x9f);
        buf.put((byte) 0x04);
        buf.put((byte) 0x05);
        buf.put((byte) 0xff);
        buf.put((byte) 0xff);
        buf.flip();

        var item = new CborArrayDecoder<Object, ArrayList<Object>>(Collectors.arrayList(), new CborVariantDecoder<>(List.of(
                CborPrim.UNSIGNED,
                new CborArrayDecoder<>(Collectors.arrayList(), CborPrim.UNSIGNED)
        )));
        var x = Cbor.decode(buf, item);
        assertEquals(3, x.size());
        assertEquals(1, (long) (Long) x.get(0));
        var arr1 = assertInstanceOf(ArrayList.class, x.get(1));
        var arr2 = assertInstanceOf(ArrayList.class, x.get(2));
        assertEquals(2, arr1.size());
        assertEquals(2, (long) (Long) arr1.get(0));
        assertEquals(3, (long) (Long) arr1.get(1));
        assertEquals(2, arr2.size());
        assertEquals(4, (long) (Long) arr2.get(0));
        assertEquals(5, (long) (Long) arr2.get(1));
    }

    @Test
    public void nestedMixedIndefiniteArraysVariant2() {
        var buf = ByteBuffer.allocate(64);
        buf.put((byte) 0x83);
        buf.put((byte) 0x01);
        buf.put((byte) 0x82);
        buf.put((byte) 0x02);
        buf.put((byte) 0x03);
        buf.put((byte) 0x9f);
        buf.put((byte) 0x04);
        buf.put((byte) 0x05);
        buf.put((byte) 0xff);
        buf.flip();

        var item = new CborArrayDecoder<Object, ArrayList<Object>>(Collectors.arrayList(), new CborVariantDecoder<>(List.of(
                CborPrim.UNSIGNED,
                new CborArrayDecoder<>(Collectors.arrayList(), CborPrim.UNSIGNED)
        )));
        var x = Cbor.decode(buf, item);
        assertEquals(3, x.size());
        assertEquals(1, (long) (Long) x.get(0));
        var arr1 = assertInstanceOf(ArrayList.class, x.get(1));
        var arr2 = assertInstanceOf(ArrayList.class, x.get(2));
        assertEquals(2, arr1.size());
        assertEquals(2, (long) (Long) arr1.get(0));
        assertEquals(3, (long) (Long) arr1.get(1));
        assertEquals(2, arr2.size());
        assertEquals(4, (long) (Long) arr2.get(0));
        assertEquals(5, (long) (Long) arr2.get(1));
    }

    @Test
    public void indefiniteStringIntMap() {
        var buf = ByteBuffer.allocate(64);
        buf.put((byte) 0xbf);
        buf.put((byte) 0x63);
        buf.put((byte) 0x46);
        buf.put((byte) 0x75);
        buf.put((byte) 0x6e);
        buf.put((byte) 0xf5);
        buf.put((byte) 0x63);
        buf.put((byte) 0x41);
        buf.put((byte) 0x6d);
        buf.put((byte) 0x74);
        buf.put((byte) 0x21);
        buf.put((byte) 0xff);
        buf.flip();

        var item = new CborMapDecoder<>(
                Collectors.map(HashMap::new),
                CborPrim.STRING,
                new CborVariantDecoder<>(List.of(
                    CborPrim.SIGNED,
                    CborPrim.BOOL
                )));
        var x = Cbor.decode(buf, item);
        assertEquals(2, x.size());
        assertTrue((Boolean) x.get("Fun"));
        assertEquals(-2, (long) (Long) x.get("Amt"));
    }

    @Test
    public void seq() {
        var buf = ByteBuffer.allocate(64);
        buf.put((byte) 0x02);
        buf.put((byte) 0x03);
        buf.put((byte) 0x04);
        buf.flip();

        var item = CborPrim.UNSIGNED;
        var seq = new CborSeq(buf);
        assertTrue(seq.hasNext());
        assertEquals(2, seq.next(item));
        assertTrue(seq.hasNext());
        assertEquals(3, seq.next(item));
        assertTrue(seq.hasNext());
        assertEquals(4, seq.next(item));
        assertFalse(seq.hasNext());
    }

    @Test
    public void chain() {
        var buf = ByteBuffer.allocate(64);
        buf.put((byte) 0x02);
        buf.put((byte) 0x03);
        buf.flip();

        CborItemDecoder<Long> item = CborItemDecoder.chain(CborPrim.UNSIGNED, CborPrim.UNSIGNED, Long::sum);
        var seq = new CborSeq(buf);
        assertTrue(seq.hasNext());
        assertEquals(2 + 3, seq.next(item));
        assertFalse(seq.hasNext());
    }

    @Test
    public void map() {
        var buf = ByteBuffer.allocate(64);
        buf.put((byte) 0x02);
        buf.flip();

        CborItemDecoder<Integer> item = CborItemDecoder.map(CborPrim.UNSIGNED, x -> (Integer) (int) (long) x);
        var seq = new CborSeq(buf);
        assertTrue(seq.hasNext());
        assertEquals(2, seq.next(item));
        assertFalse(seq.hasNext());
    }*/
}
