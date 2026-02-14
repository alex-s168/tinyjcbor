package dev.vxcc.tinyjcbor.serde;

import dev.vxcc.tinyjcbor.CborDecoder;
import dev.vxcc.tinyjcbor.CborEncoder;
import dev.vxcc.tinyjcbor.CborType;
import dev.vxcc.tinyjcbor.UnexpectedCborException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;

public final class CborPrim {
    /**
     * A CBOR deserializer that accepts only the given types (but does not check!)
     *
     * @since 1.0.0-rc.1
     */
    public static abstract class PrimitiveDecoder<T> implements CborDeserializer<T> {
        private final @NotNull CborType @NotNull[] accepts;

        public PrimitiveDecoder(@NotNull CborType @NotNull[] accepts) {
            this.accepts = accepts;
        }

        @Override
        public final boolean mightAccept(@NotNull CborType type) {
           for (var t : accepts)
               if (type == t)
                   return true;
           return false;
        }

        @Override
        public final boolean neverAccepts(@NotNull CborType type) {
            return !mightAccept(type);
        }

        /**
         * @return Array of types this deserializer accepts
         */
        public @NotNull CborType @NotNull[] getTypes() {
            return accepts;
        }
    }

    public static final CborSerDe<@NotNull Boolean> BOOL =
        new CborSerDe<>(
            new PrimitiveDecoder<@NotNull Boolean>(new CborType[]{ CborType.True, CborType.False })
    {
        @Override
        public Boolean next(@NotNull CborDecoder decoder) throws UnexpectedCborException {
            return decoder.readBool();
        }
    }, CborEncoder::writeBool);

    public static final CborSerDe<@NotNull Long> SIGNED =
        new CborSerDe<>(
            new PrimitiveDecoder<@NotNull Long>(new CborType[]{ CborType.UnsignedInteger, CborType.NegativeInteger })
    {
        @Override
        public Long next(@NotNull CborDecoder decoder) throws UnexpectedCborException {
            return decoder.readInt();
        }
    }, CborEncoder::writeSigned);

    public static final CborSerDe<@NotNull Long> UNSIGNED =
        new CborSerDe<>(
            new PrimitiveDecoder<@NotNull Long>(new CborType[]{ CborType.UnsignedInteger })
    {
        @Override
        public Long next(@NotNull CborDecoder decoder) throws UnexpectedCborException {
            return decoder.readUInt();
        }
    }, CborEncoder::writeUnsigned);

    public static final CborSerDe<@Nullable Void> NULL =
        new CborSerDe<>(
            new PrimitiveDecoder<@Nullable Void>(new CborType[]{ CborType.Null })
    {
        @Override
        public @Nullable Void next(@NotNull CborDecoder decoder) throws UnexpectedCborException {
            decoder.readNull();
            return null;
        }
    }, (out, x) -> out.writeNull());

    public static final CborSerDe<@Nullable Void> UNDEFINED =
        new CborSerDe<>(
            new PrimitiveDecoder<@Nullable Void>(new CborType[]{ CborType.Undefined })
    {
        @Override
        public @Nullable Void next(@NotNull CborDecoder decoder) throws UnexpectedCborException {
            decoder.readUndefined();
            return null;
        }
    }, (out, x) -> out.writeUndefined());

    public static final CborSerDe<@NotNull Short> HALF =
        new CborSerDe<>(
            new PrimitiveDecoder<@NotNull Short>(new CborType[]{ CborType.Float16 })
    {
        @Override
        public Short next(@NotNull CborDecoder decoder) throws UnexpectedCborException {
            return decoder.readFloat16();
        }
    }, CborEncoder::writeFloat16);

    public static final CborSerDe<@NotNull Float> HALF_TO_FLOAT =
        new CborSerDe<>(
            new PrimitiveDecoder<@NotNull Float>(new CborType[]{ CborType.Float16 })
    {
        @Override
        public Float next(@NotNull CborDecoder decoder) throws UnexpectedCborException {
            return Float.float16ToFloat(decoder.readFloat16());
        }
    }, (out, x) -> out.writeFloat16(Float.floatToFloat16(x)));

    public static final CborSerDe<@NotNull Float> FLOAT =
        new CborSerDe<>(
            new PrimitiveDecoder<@NotNull Float>(new CborType[]{ CborType.Float32 })
    {
        @Override
        public Float next(@NotNull CborDecoder decoder) throws UnexpectedCborException {
            return decoder.readFloat32();
        }
    }, CborEncoder::writeFloat32);

    public static final CborSerDe<@NotNull Double> DOUBLE =
        new CborSerDe<>(
            new PrimitiveDecoder<@NotNull Double>(new CborType[]{ CborType.Float64 })
    {
        @Override
        public Double next(@NotNull CborDecoder decoder) throws UnexpectedCborException {
            return decoder.readFloat64();
        }
    }, CborEncoder::writeFloat64);

    public static final CborSerDe<@NotNull Float> MOST_FLOAT =
        new CborSerDe<>(
            new PrimitiveDecoder<@NotNull Float>(new CborType[]{ CborType.Float16, CborType.Float32 })
    {
        @Override
        public Float next(@NotNull CborDecoder decoder) throws UnexpectedCborException, dev.vxcc.tinyjcbor.InvalidCborException {
            var type = decoder.peekTokenType();
            return switch (type) {
                case Float16 -> Float.float16ToFloat(decoder.readFloat16());
                case Float32 -> decoder.readFloat32();
                default -> throw new UnexpectedCborException.UnexpectedType(CborType.Float16.name() + " or " + CborType.Float32.name(), type);
            };
        }
    }, CborEncoder::writeFloat32);

    public static final CborSerDe<@NotNull Double> MOST_DOUBLE =
        new CborSerDe<>(
            new PrimitiveDecoder<@NotNull Double>(new CborType[]{ CborType.Float16, CborType.Float32, CborType.Float32 })
    {
        @Override
        public Double next(@NotNull CborDecoder decoder) throws UnexpectedCborException, dev.vxcc.tinyjcbor.InvalidCborException {
            var type = decoder.peekTokenType();
            return switch (type) {
                case Float16 -> (double) Float.float16ToFloat(decoder.readFloat16());
                case Float32 -> (double) decoder.readFloat32();
                case Float64 -> decoder.readFloat64();
                default -> throw new UnexpectedCborException.UnexpectedType(CborType.Float16.name() + " or " + CborType.Float32.name() + " or " + CborType.Float64.name(), type);
            };
        }
    }, CborEncoder::writeFloat64);

    public static final CborSerDe<byte @NotNull []> BYTE_ARRAY =
        new CborSerDe<>(
            new CborByteArrayDecoder<>(CborDecoder.ByteArrayReader::readAll),
            CborEncoder::writeByteString);

    public static final CborSerDe<@NotNull String> STRING =
        new CborSerDe<>(
            new CborStringDecoder<@NotNull String>(){
                @Override
                protected @NotNull String process(Reader x) {
                    try {
                        var out = new StringBuilder();
                        var buf = new char[512];
                        int n;
                        while ((n = x.read(buf)) > 0) {
                            out.append(buf, 0, n);
                        }
                        return out.toString();
                    } catch (IOException e) {
                        throw new RuntimeException(e); /* how even */
                    }
                }
            },
            CborEncoder::writeText);
}
