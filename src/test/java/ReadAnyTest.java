import dev.vxcc.tinyjcbor.Cbor;
import dev.vxcc.tinyjcbor.CborDecoder;
import dev.vxcc.tinyjcbor.CborEncoder;
import dev.vxcc.tinyjcbor.serde.*;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ReadAnyTest {

    @Test
    public void taggedIntArray() {
        var buf = ByteBuffer.allocate(64);
        buf.put((byte) 0xc4);
        buf.put((byte) 0x82);
        buf.put((byte) 0x21);
        buf.put((byte) 0x19);
        buf.putShort((short) 0x6ab3);
        buf.flip();

        new CborDecoder(buf).readAny();
        assertEquals(0, buf.remaining());
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

        new CborDecoder(buf).readAny();
        assertEquals(0, buf.remaining());
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

        new CborDecoder(buf).readAny();
        assertEquals(0, buf.remaining());
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

        new CborDecoder(buf).readAny();
        assertEquals(0, buf.remaining());
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

        new CborDecoder(buf).readAny();
        assertEquals(0, buf.remaining());
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

        new CborDecoder(buf).readAny();
        assertEquals(0, buf.remaining());
    }

    @Test
    public void recodeF16() {
        var val = Float.floatToFloat16((float) Math.PI);
        var serde = CborPrim.HALF;
        var bytes = Cbor.encode(ByteOrder.BIG_ENDIAN, val, serde);
        var buf = ByteBuffer.wrap(bytes);
        new CborDecoder(buf).readAny();
        assertEquals(0, buf.remaining());
    }

    @Test
    public void recodeF32() {
        var val = (float) Math.PI;
        var serde = CborPrim.FLOAT;
        var bytes = Cbor.encode(ByteOrder.BIG_ENDIAN, val, serde);
        var buf = ByteBuffer.wrap(bytes);
        new CborDecoder(buf).readAny();
        assertEquals(0, buf.remaining());
    }

    @Test
    public void recodeF64() {
        var val = Math.PI;
        var serde = CborPrim.DOUBLE;
        var bytes = Cbor.encode(ByteOrder.BIG_ENDIAN, val, serde);
        var buf = ByteBuffer.wrap(bytes);
        new CborDecoder(buf).readAny();
        assertEquals(0, buf.remaining());
    }

    @Test
    public void finiteMap() {
        var data = Map.of(
                "some", 1L,
                "data", 2L,
                "here", 1240124L
        );
        var buf = ByteBuffer.wrap(Cbor.encode(
                ByteOrder.BIG_ENDIAN,
                data,
                new CborMapEncoder<>(CborPrim.STRING, CborPrim.UNSIGNED)
        ));
        new CborDecoder(buf).readAny();
        assertEquals(0, buf.remaining());
    }

    @Test
    public void finiteMapDecodeManual() {
        var data = new LinkedHashMap<String, Long>();
        data.put("some", 1L);
        data.put("data", 2L);
        data.put("here", 1240124L);

        var buf = ByteBuffer.wrap(Cbor.encode(
                ByteOrder.BIG_ENDIAN,
                data,
                new CborMapEncoder<>(CborPrim.STRING, CborPrim.UNSIGNED)
        ));

        new CborDecoder(buf).readAny();
        assertEquals(0, buf.remaining());
    }

    @Test
    public void indefiniteMapDecodeManual() throws IOException {
        var out = new ByteArrayOutputStream();
        var enc = new CborEncoder(ByteOrder.BIG_ENDIAN, out);

        var encMap = enc.writeMap();
        enc.writeText("some");
        enc.writeSigned(1L);
        enc.writeText("data");
        enc.writeSigned(2L);
        enc.writeText("here");
        enc.writeSigned(1240124L);
        encMap.end();
        var buf = ByteBuffer.wrap(out.toByteArray());

        new CborDecoder(buf).readAny();
        assertEquals(0, buf.remaining());
    }

    @Test
    public void indefiniteArrayDecodeManual() throws IOException {
        var out = new ByteArrayOutputStream();
        var enc = new CborEncoder(ByteOrder.BIG_ENDIAN, out);

        var encMap = enc.writeArray();
        enc.writeText("some");
        enc.writeText("data");
        enc.writeText("here");
        encMap.end();
        var buf = ByteBuffer.wrap(out.toByteArray());

        new CborDecoder(buf).readAny();
        assertEquals(0, buf.remaining());
    }

    @Test
    public void finiteArrDecodeManual() {
        var buf = ByteBuffer.wrap(Cbor.encode(
                ByteOrder.BIG_ENDIAN,
                List.of("some", "data", "here"),
                new CborCollectionArrayEncoder<>(CborPrim.STRING)
        ));

        new CborDecoder(buf).readAny();
        assertEquals(0, buf.remaining());
    }

    @Test
    public void indefiniteArray() {
        var data = Set.of(
                "some",
                "data",
                "here"
        );

        var buf = ByteBuffer.wrap(Cbor.encode(
                ByteOrder.BIG_ENDIAN,
                data,
                new CborIndefiniteArrayEncoder<>(CborPrim.STRING)
        ));
        new CborDecoder(buf).readAny();
        assertEquals(0, buf.remaining());
    }
}
