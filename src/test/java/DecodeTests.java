import dev.vxcc.tinyjcbor.Cbor;
import dev.vxcc.tinyjcbor.CborSeq;
import dev.vxcc.tinyjcbor.serde.*;
import dev.vxcc.tinyjcbor.util.MapConstructor;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class DecodeTests {
    @Test
    public void ensureByteBufBigEndian() {
        var buf = ByteBuffer.allocate(1);
        assertEquals(ByteOrder.BIG_ENDIAN, buf.order());
    }

    @Test
    public void taggedIntArray() {
        var buf = ByteBuffer.allocate(64);
        buf.put((byte) 0xc4);
        buf.put((byte) 0x82);
        buf.put((byte) 0x21);
        buf.put((byte) 0x19);
        buf.putShort((short) 0x6ab3);
        buf.flip();

        var item = new CborFixedTagDecoder<>(4, new CborArrayDecoder<>(Collectors.toList(), CborPrim.SIGNED));
        var x = Cbor.decode(buf, item);
        assertEquals(0, buf.remaining());
        assertEquals(2, x.size());
        assertEquals(-2, x.get(0));
        assertEquals(27315, x.get(1));
    }

    @Test
    public void indefiniteLengthByteString() {
        var buf = ByteBuffer.allocate(64);
        buf.put((byte) 0x5f);
        buf.put((byte) 0x44);
        buf.put((byte) 0xaa);
        buf.put((byte) 0xbb);
        buf.put((byte) 0xcc);
        buf.put((byte) 0xdd);
        buf.put((byte) 0x43);
        buf.put((byte) 0xee);
        buf.put((byte) 0xff);
        buf.put((byte) 0x99);
        buf.put((byte) 0xff);
        buf.flip();

        var x = Cbor.decode(buf, CborPrim.BYTE_ARRAY);
        assertEquals(0, buf.remaining());
        assertEquals(7, x.length);
        assertEquals((byte) 0xaa, x[0]);
        assertEquals((byte) 0xbb, x[1]);
        assertEquals((byte) 0xcc, x[2]);
        assertEquals((byte) 0xdd, x[3]);
        assertEquals((byte) 0xee, x[4]);
        assertEquals((byte) 0xff, x[5]);
        assertEquals((byte) 0x99, x[6]);
    }

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

        var item = new CborArrayDecoder<>(Collectors.toList(), new CborVariantDecoder<>(List.of(
                CborPrim.UNSIGNED,
                new CborArrayDecoder<>(Collectors.toList(), CborPrim.UNSIGNED)
        )));
        var x = Cbor.decode(buf, item);
        assertEquals(0, buf.remaining());
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

    /** like nestedFiniteLengthArraysVariant, but hits [CborDecoder#reset], by making [CborVariantDecoder]'s opeations less efficient */
    @Test
    public void nestedFiniteLengthArraysVariantHitDecoderReset() {
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

        var item = new CborArrayDecoder<>(Collectors.toList(), new CborVariantDecoder<>(List.of(
                new LessInfoDecoder<>(CborPrim.UNSIGNED),
                new LessInfoDecoder<>(new CborArrayDecoder<>(Collectors.toList(), CborPrim.UNSIGNED))
        )));
        var x = Cbor.decode(buf, item);
        assertEquals(0, buf.remaining());
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

        var item = new CborArrayDecoder<>(Collectors.toList(), new CborVariantDecoder<>(List.of(
                CborPrim.UNSIGNED,
                new CborArrayDecoder<>(Collectors.toList(), CborPrim.UNSIGNED)
        )));
        var x = Cbor.decode(buf, item);
        assertEquals(0, buf.remaining());
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

        var item = new CborArrayDecoder<>(Collectors.toList(), new CborVariantDecoder<>(List.of(
                CborPrim.UNSIGNED,
                new CborArrayDecoder<>(Collectors.toList(), CborPrim.UNSIGNED)
        )));
        var x = Cbor.decode(buf, item);
        assertEquals(0, buf.remaining());
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
                MapConstructor.map(HashMap::new),
                CborPrim.STRING,
                new CborVariantDecoder<>(List.of(
                    CborPrim.SIGNED,
                    CborPrim.BOOL
                )));
        var x = Cbor.decode(buf, item);
        assertEquals(0, buf.remaining());
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
        assertEquals(0, buf.remaining());
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
        assertEquals(0, buf.remaining());
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
        assertEquals(0, buf.remaining());
    }

    // TODO: test tagged decoder
}
