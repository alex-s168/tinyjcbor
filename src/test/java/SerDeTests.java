import dev.vxcc.tinyjcbor.Cbor;
import dev.vxcc.tinyjcbor.CborDecoder;
import dev.vxcc.tinyjcbor.CborEncoder;
import dev.vxcc.tinyjcbor.serde.*;
import dev.vxcc.tinyjcbor.util.MapConstructor;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SerDeTests {
    @Test
    public void recodeF16() {
        var val = Float.floatToFloat16((float) Math.PI);
        var serde = CborPrim.HALF;
        var bytes = Cbor.encode(ByteOrder.BIG_ENDIAN, val, serde);
        assertEquals(3, bytes.length);
        assertEquals(-7, bytes[0]);
        assertEquals(66, bytes[1]);
        assertEquals(72, bytes[2]);
        var buf = ByteBuffer.wrap(bytes);
        var decodedVal = Cbor.decode(buf, serde);
        assertEquals(0, buf.remaining());
        assertEquals(val, decodedVal);
        assertEquals(Float.float16ToFloat(val), Cbor.decode(ByteBuffer.wrap(bytes), CborPrim.HALF_TO_FLOAT));
        assertEquals(Float.float16ToFloat(val), Cbor.decode(ByteBuffer.wrap(bytes), CborPrim.MOST_FLOAT));
        assertEquals(Float.float16ToFloat(val), Cbor.decode(ByteBuffer.wrap(bytes), CborPrim.MOST_DOUBLE));
    }

    @Test
    public void recodeF32() {
        var val = (float) Math.PI;
        var serde = CborPrim.FLOAT;
        var bytes = Cbor.encode(ByteOrder.BIG_ENDIAN, val, serde);
        assertEquals(5, bytes.length);
        assertEquals(-6, bytes[0]);
        assertEquals(64, bytes[1]);
        assertEquals(73, bytes[2]);
        assertEquals(15, bytes[3]);
        assertEquals(-37, bytes[4]);
        var buf = ByteBuffer.wrap(bytes);
        var decodedVal = Cbor.decode(buf, serde);
        assertEquals(0, buf.remaining());
        assertEquals(val, decodedVal);
        assertEquals((float) Math.PI, Cbor.decode(ByteBuffer.wrap(bytes), CborPrim.MOST_FLOAT));
        assertEquals((float) Math.PI, Cbor.decode(ByteBuffer.wrap(bytes), CborPrim.MOST_DOUBLE));
    }

    @Test
    public void recodeF64() {
        var val = Math.PI;
        var serde = CborPrim.DOUBLE;
        var bytes = Cbor.encode(ByteOrder.BIG_ENDIAN, val, serde);
        assertEquals(9, bytes.length);
        assertEquals(-5, bytes[0]);
        assertEquals(64, bytes[1]);
        assertEquals(9, bytes[2]);
        assertEquals(33, bytes[3]);
        assertEquals(-5, bytes[4]);
        assertEquals(84, bytes[5]);
        assertEquals(68, bytes[6]);
        assertEquals(45, bytes[7]);
        assertEquals(24, bytes[8]);
        var buf = ByteBuffer.wrap(bytes);
        var decodedVal = Cbor.decode(buf, serde);
        assertEquals(0, buf.remaining());
        assertEquals(val, decodedVal);
        assertEquals(Math.PI, Cbor.decode(ByteBuffer.wrap(bytes), CborPrim.MOST_DOUBLE));
    }

    @Test
    public void recodeSpecial() {
        assertEquals(false, Cbor.decode(ByteBuffer.wrap(Cbor.encode(ByteOrder.BIG_ENDIAN, false, CborPrim.BOOL)), CborPrim.BOOL));
        assertEquals(true, Cbor.decode(ByteBuffer.wrap(Cbor.encode(ByteOrder.BIG_ENDIAN, true, CborPrim.BOOL)), CborPrim.BOOL));
        Cbor.decode(ByteBuffer.wrap(Cbor.encode(ByteOrder.BIG_ENDIAN, null, CborPrim.NULL)), CborPrim.NULL);
        Cbor.decode(ByteBuffer.wrap(Cbor.encode(ByteOrder.BIG_ENDIAN, null, CborPrim.UNDEFINED)), CborPrim.UNDEFINED);
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
        var dec = Cbor.decode(buf,
                new CborMapDecoder<>(
                        MapConstructor.mapDowncast(HashMap::new),
                        CborPrim.STRING, CborPrim.UNSIGNED)
                );
        assertEquals(0, buf.remaining());
        assertEquals(data.entrySet(), dec.entrySet());
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

        var decoder = new CborDecoder(buf);
        var map = decoder.readMapManual();
        map.next();
        assertEquals("some", decoder.read(CborPrim.STRING));
        assertEquals(1L, decoder.readInt());
        map.next();
        assertEquals("data", decoder.read(CborPrim.STRING));
        assertEquals(2L, decoder.readInt());
        map.next();
        assertEquals("here", decoder.read(CborPrim.STRING));
        assertEquals(1240124L, decoder.readInt());
        map.end();

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

        var decoder = new CborDecoder(buf);
        var map = decoder.readMapManual();
        map.next();
        assertEquals("some", decoder.read(CborPrim.STRING));
        assertEquals(1L, decoder.readInt());
        map.next();
        assertEquals("data", decoder.read(CborPrim.STRING));
        assertEquals(2L, decoder.readInt());
        map.next();
        assertEquals("here", decoder.read(CborPrim.STRING));
        assertEquals(1240124L, decoder.readInt());
        map.end();

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

        var decoder = new CborDecoder(buf);
        var arr = decoder.readArrayManual();
        arr.next();
        assertEquals("some", decoder.read(CborPrim.STRING));
        arr.next();
        assertEquals("data", decoder.read(CborPrim.STRING));
        arr.next();
        assertEquals("here", decoder.read(CborPrim.STRING));
        arr.end();

        assertEquals(0, buf.remaining());
    }

    @Test
    public void finiteArrDecodeManual() {
        var buf = ByteBuffer.wrap(Cbor.encode(
                ByteOrder.BIG_ENDIAN,
                List.of("some", "data", "here"),
                new CborCollectionArrayEncoder<>(CborPrim.STRING)
        ));

        var decoder = new CborDecoder(buf);
        var map = decoder.readArrayManual();
        map.next();
        assertEquals("some", decoder.read(CborPrim.STRING));
        map.next();
        assertEquals("data", decoder.read(CborPrim.STRING));
        map.next();
        assertEquals("here", decoder.read(CborPrim.STRING));
        map.end();

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
        var dec = Cbor.decode(buf,
                new CborArrayDecoder<>(
                        Collectors.toSet(),
                        CborPrim.STRING)
        );
        assertEquals(0, buf.remaining());
        assertEquals(data, dec);
    }

    @Test
    public void chunkedByteString() throws IOException {
        var out = new ByteArrayOutputStream();
        var enc = new CborEncoder(ByteOrder.BIG_ENDIAN, out);

        var x = enc.writeChunkedByteString();
        x.writeChunk(new byte[]{ 1, 2, 3 });
        x.writeChunk(new byte[]{ 4, 5 });
        x.end();

        var buf = ByteBuffer.wrap(out.toByteArray());
        var dec = Cbor.decode(buf, CborPrim.BYTE_ARRAY);
        assertEquals(5, dec.length);
        assertEquals(1, dec[0]);
        assertEquals(2, dec[1]);
        assertEquals(3, dec[2]);
        assertEquals(4, dec[3]);
        assertEquals(5, dec[4]);
        assertEquals(0, buf.remaining());
    }

    @Test
    public void chunkedText() throws IOException {
        var out = new ByteArrayOutputStream();
        var enc = new CborEncoder(ByteOrder.BIG_ENDIAN, out);

        var x = enc.writeChunkedText();
        x.writeChunk("hello");
        x.writeChunk(" ");
        x.writeChunk("world");
        x.end();

        var buf = ByteBuffer.wrap(out.toByteArray());
        var dec = Cbor.decode(buf, CborPrim.STRING);
        assertEquals("hello world", dec);
        assertEquals(0, buf.remaining());
    }
}
