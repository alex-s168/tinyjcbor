package dev.vxcc.tinyjcbor.util;

import dev.vxcc.tinyjcbor.*;
import dev.vxcc.tinyjcbor.serde.CborDeserializer;
import dev.vxcc.tinyjcbor.serde.CborPrim;
import dev.vxcc.tinyjcbor.serde.CborSerializer;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

/**
 * Represents any CBOR value
 * @since 1.0.0-rc.3
 */
public sealed abstract class CborValue {
    @Override
    public abstract boolean equals(Object obj);

    @Override
    public abstract int hashCode();

    @Override
    public abstract String toString();

    public abstract CborType type();

    /**
     * Converts this value into a decoder, so it can be deserialized.
     * <p>This is currently inefficient!
     *
     * @since 1.0.0-rc.3
     */
    public final CborDecoder asDecoder() {
        var order = ByteOrder.BIG_ENDIAN;
        var arr = Cbor.encode(order, this, CODEC);
        var buf = ByteBuffer.wrap(arr);
        buf.order(order);
        return new CborDecoder(buf);
    }

    public static final class Unsigned extends CborValue {
        public final long value;

        public Unsigned(long value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return Long.toString(value);
        }

        @Override
        public int hashCode() {
            return Long.hashCode(value);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Unsigned u)
                return value == u.value;
            return false;
        }

        @Override
        public CborType type() {
            return CborType.UnsignedInteger;
        }
    }

    public static final class Signed extends CborValue {
        public final long value;

        public Signed(long value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return Long.toString(value);
        }

        @Override
        public int hashCode() {
            return Long.hashCode(value);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Signed u)
                return value == u.value;
            return false;
        }

        @Override
        public CborType type() {
            return value < 0 ? CborType.NegativeInteger : CborType.UnsignedInteger;
        }
    }

    public static final class Float16 extends CborValue {
        public final short value;

        public Float16(short value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return Float.toString(Float.float16ToFloat(value));
        }

        @Override
        public int hashCode() {
            return Short.hashCode(value);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Float16 u)
                return value == u.value;
            return false;
        }

        @Override
        public CborType type() {
            return CborType.Float16;
        }
    }

    public static final class Float32 extends CborValue {
        public final float value;

        public Float32(float value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return Float.toString(value);
        }

        @Override
        public int hashCode() {
            return Float.hashCode(value);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Float32 u)
                return value == u.value;
            return false;
        }

        @Override
        public CborType type() {
            return CborType.Float32;
        }
    }

    public static final class Float64 extends CborValue {
        public final double value;

        public Float64(double value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return Double.toString(value);
        }

        @Override
        public int hashCode() {
            return Double.hashCode(value);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Float64 u)
                return value == u.value;
            return false;
        }

        @Override
        public CborType type() {
            return CborType.Float64;
        }
    }

    public static final class Str extends CborValue {
        public final String value;

        public Str(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "\"" + value + "\"";
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Str u)
                return Objects.deepEquals(value, u.value);
            return false;
        }

        @Override
        public CborType type() {
            return CborType.Text;
        }
    }

    public static final class Bytes extends CborValue {
        public final byte[] value;

        public Bytes(byte[] value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return HexFormat.of().formatHex(value);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(value);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Bytes u)
                return Arrays.equals(value, u.value);
            return false;
        }

        @Override
        public CborType type() {
            return CborType.ByteString;
        }
    }

    public static final class Arr extends CborValue {
        public final List<CborValue> value;

        public Arr(List<CborValue> value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value.toString();
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Arr u)
                return Objects.deepEquals(value, u.value);
            return false;
        }

        @Override
        public CborType type() {
            return CborType.Array;
        }
    }

    public static final class Dict extends CborValue {
        public final Map<CborValue, CborValue> value;

        public Dict(Map<CborValue, CborValue> value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value.toString();
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Dict u)
                return Objects.deepEquals(value, u.value);
            return false;
        }

        @Override
        public CborType type() {
            return CborType.Map;
        }
    }

    public static final class Tag extends CborValue {
        public final long tag;
        public final CborValue value;

        public Tag(long tag, CborValue value) {
            this.tag = tag;
            this.value = value;
        }

        @Override
        public String toString() {
            return "#" + tag + ".(" + value + ")";
        }

        @Override
        public int hashCode() {
            return Long.hashCode(tag) ^ value.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Tag u)
                return tag == u.tag && Objects.equals(value, u.value);
            return false;
        }

        @Override
        public CborType type() {
            return CborType.Tag;
        }
    }

    public static final class Bool extends CborValue {
        public final boolean value;

        public Bool(boolean value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value ? "true" : "false";
        }

        public static final Bool TRUE = new Bool(true);
        public static final Bool FALSE = new Bool(false);

        @Override
        public int hashCode() {
            return Boolean.hashCode(value);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Bool u)
                return value == u.value;
            return false;
        }

        @Override
        public CborType type() {
            return value ? CborType.True : CborType.False;
        }
    }

    public static final class Null extends CborValue {
        public Null() {}

        @Override
        public String toString() {
            return "null";
        }

        public static final Null VALUE = new Null();

        @Override
        public int hashCode() {
            return 69696969;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Null;
        }

        @Override
        public CborType type() {
            return CborType.Null;
        }
    }

    public static final class Undefined extends CborValue {
        public Undefined() {}

        @Override
        public String toString() {
            return "undefined";
        }

        public static final Undefined VALUE = new Undefined();

        @Override
        public int hashCode() {
            return 6969;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Undefined;
        }

        @Override
        public CborType type() {
            return CborType.Undefined;
        }
    }

    public static final class Simple extends CborValue {
        public final byte value;

        public Simple(byte value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "simple(" + value + ")";
        }

        @Override
        public int hashCode() {
            return Byte.hashCode(value);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Simple u)
                return value == u.value;
            return false;
        }

        @Override
        public CborType type() {
            return CborType.SimpleValue;
        }
    }

    public static final class Codec implements CborDeserializer<CborValue>, CborSerializer<CborValue> {
        private Codec() {}

        @Override
        public CborValue next(@NotNull CborDecoder decoder) throws UnexpectedCborException {
            return switch (decoder.peekTokenType()) {
                case UnsignedInteger -> new Unsigned(decoder.readUInt());
                case NegativeInteger -> new Signed(decoder.readInt());
                case SimpleValue -> new Simple(decoder.readSimple());
                case False, True -> new Bool(decoder.readBool());
                case Null -> Null.VALUE;
                case Undefined -> Undefined.VALUE;
                case Float16 -> new Float16(decoder.readFloat16());
                case Float32 -> new Float32(decoder.readFloat32());
                case Float64 -> new Float64(decoder.readFloat64());
                case ByteString -> new Bytes(decoder.read(CborPrim.BYTES));
                case Text -> new Str(decoder.read(CborPrim.STRING));
                case Tag -> {
                    var tag = decoder.readTag();
                    yield new Tag(tag, next(decoder));
                }
                case Array -> {
                    var out = new ArrayList<CborValue>();
                    var arr = decoder.readArrayManual();
                    while (arr.hasNext()) {
                        arr.next();
                        out.add(next(decoder));
                    }
                    yield new Arr(out);
                }
                case Map -> {
                    var out = new HashMap<CborValue, CborValue>();
                    var arr = decoder.readArrayManual();
                    while (arr.hasNext()) {
                        arr.next();
                        var k = next(decoder);
                        var v = next(decoder);
                        out.put(k, v);
                    }
                    yield new Dict(out);
                }
                case Break -> throw new UnexpectedCborException.Custom("expected value");
                case null -> throw new UnexpectedCborException.Custom("expected value");
            };
        }

        @Override
        public void encode(@NotNull CborEncoder encoder, CborValue value) throws IOException {
            switch (value) {
                case Arr arr -> {
                    var w = encoder.writeArray();
                    for (var x : arr.value) {
                        encode(encoder, x);
                    }
                    w.end();
                }
                case Dict dict -> {
                    var w = encoder.writeArray();
                    for (var x : dict.value.entrySet()) {
                        encode(encoder, x.getKey());
                        encode(encoder, x.getValue());
                    }
                    w.end();
                }
                case Bytes bytes -> encoder.writeByteString(bytes.value);
                case Float16 n -> encoder.writeFloat16(n.value);
                case Float32 n -> encoder.writeFloat32(n.value);
                case Float64 n -> encoder.writeFloat64(n.value);
                case Signed signed -> encoder.writeSigned(signed.value);
                case Unsigned unsigned -> encoder.writeUnsigned(unsigned.value);
                case Str str -> encoder.writeText(str.value);
                case Tag tag -> {
                    encoder.writeTag(tag.tag);
                    encode(encoder, tag.value);
                }
                case Undefined ignored -> encoder.writeUndefined();
                case Null ignored -> encoder.writeNull();
                case Bool b -> encoder.writeBool(b.value);
                case Simple s -> encoder.writeSimple(s.value);
            }
        }
    }

    public static final Codec CODEC = new Codec();
}
