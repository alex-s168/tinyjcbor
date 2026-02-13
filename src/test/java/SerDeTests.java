import dev.vxcc.tinyjcbor.Cbor;
import dev.vxcc.tinyjcbor.serde.*;
import dev.vxcc.tinyjcbor.util.Collectors;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
                        Collectors.mapDowncast(HashMap::new),
                        CborPrim.STRING, CborPrim.UNSIGNED)
                );
        assertEquals(0, buf.remaining());
        assertEquals(data.entrySet(), dec.entrySet());
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
                        Collectors.hashSet(),
                        CborPrim.STRING)
        );
        assertEquals(0, buf.remaining());
        assertEquals(data, dec);
    }
}
