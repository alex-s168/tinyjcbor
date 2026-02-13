package dev.vxcc.tinyjcbor;

import dev.vxcc.tinyjcbor.serde.CborItemDecoder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/** Uses the buffer's byte order as network order for integers & floats */
public final class CborDecoder {
    @NotNull
    private final ByteBuffer buffer;

    private long tokenArg;
    private int tokenMajorType = 69;
    private int tokenAdditionalInfo;
    private boolean tokenIndefiniteLength;

    public CborDecoder(@NotNull ByteBuffer buffer) {
        this.buffer = buffer;
    }

    public static final class Snapshot {
        private int position;
        private long tokenArg;
        private int tokenMajorType;
        private int tokenAdditionalInfo;
        private boolean tokenIndefiniteLength;

        public void from(@NotNull CborDecoder decoder) {
            position = decoder.buffer.position();
            tokenArg = decoder.tokenArg;
            tokenMajorType = decoder.tokenMajorType;
            tokenAdditionalInfo = decoder.tokenAdditionalInfo;
            tokenIndefiniteLength = decoder.tokenIndefiniteLength;
        }
    }

    public void reset(@NotNull Snapshot snapshot) {
        buffer.position(snapshot.position);
        tokenArg = snapshot.tokenArg;
        tokenMajorType = snapshot.tokenMajorType;
        tokenAdditionalInfo = snapshot.tokenAdditionalInfo;
        tokenIndefiniteLength = snapshot.tokenIndefiniteLength;
    }

    public boolean hasNext() {
        return buffer.hasRemaining();
    }

    public void nextToken() throws InvalidCborException {
        byte firstByte = buffer.get();
        tokenMajorType = (firstByte & 0b11100000) >> 5;
        tokenAdditionalInfo = firstByte & 0b11111;

        tokenArg = 0;
        tokenIndefiniteLength = false;

        switch (tokenAdditionalInfo) {
            case 24: tokenArg = buffer.get(); break;
            case 25: tokenArg = buffer.getShort(); break;
            case 26: tokenArg = buffer.getInt(); break;
            case 27: tokenArg = buffer.getLong(); break;
            case 28:
            case 29:
            case 30: /* reserved */
                throw new InvalidCborException();
            case 31: /* no arg */
                switch (tokenMajorType) {
                    case 2:
                    case 3:
                    case 4:
                    case 5:
                        tokenIndefiniteLength = true;
                        break;
                    case 7: break;
                    default:
                        throw new InvalidCborException();
                }
                break;
            default: /* less than 24 */
                tokenArg = tokenAdditionalInfo;
                break;
        }
    }

    /** returns the type of the current token (after a call to [nextToken]) */
    public @NotNull CborType tokenType() {
        return switch (tokenMajorType) {
            case 0 -> CborType.UnsignedInteger;
            case 1 -> CborType.NegativeInteger;
            case 2 -> CborType.Bytes;
            case 3 -> CborType.Utf8String;
            case 4 -> CborType.Array;
            case 5 -> CborType.Map;
            case 6 -> CborType.Tagged;
            case 7 -> switch (tokenAdditionalInfo) {
                case 25 -> CborType.Float16;
                case 26 -> CborType.Float32;
                case 27 -> CborType.Float64;
                /* 28-30 is already ruled out by [nextToken] */
                case 31 -> CborType.Break;
                default -> switch ((int) tokenArg) {
                    case 20 -> CborType.False;
                    case 21 -> CborType.True;
                    case 22 -> CborType.Null;
                    case 23 -> CborType.Undefined;
                    default -> CborType.SimpleValue;
                };
            };
            default -> throw new RuntimeException("Forgot to call nextToken()");
        };
    }

    /**
     * this should always follow a call to [nextToken]
     * @throws UnexpectedCborException if the current token is not an indefinite length Break
     */
    public void getBreak() throws UnexpectedCborException {
        if (tokenMajorType != 7 || tokenAdditionalInfo != 31)
            throw new UnexpectedCborException.UnexpectedType(CborType.SimpleValue.name(), tokenType());
    }

    /**
     * this should always follow a call to [nextToken]
     * @return true, if the current token is not an indefinite length Break
     */
    public boolean isBreak()  {
        return tokenMajorType == 7 && tokenAdditionalInfo == 31;
    }

    /** this should always follow a call to [nextToken] */
    public byte getSimple() throws UnexpectedCborException {
        if (tokenMajorType != 7 || tokenAdditionalInfo > 24)
            throw new UnexpectedCborException.UnexpectedType(CborType.SimpleValue.name(), tokenType());
        return (byte) tokenArg;
    }

    /** this should always follow a call to [nextToken] */
    public long getUInt() throws UnexpectedCborException {
        if (tokenMajorType != 0)
            throw new UnexpectedCborException.UnexpectedType(CborType.UnsignedInteger.name(), tokenType());
        return tokenArg;
    }

    /** this should always follow a call to [nextToken] */
    public long getInt() throws UnexpectedCborException {
        return switch (tokenMajorType) {
            case 0 -> tokenArg;
            case 1 -> -1 - tokenArg;
            default ->
                    throw new UnexpectedCborException.UnexpectedType(CborType.UnsignedInteger.name() + " or " + CborType.NegativeInteger.name(), tokenType());
        };
    }

    public short getFloat16() throws UnexpectedCborException {
        if (tokenMajorType != 7 || tokenAdditionalInfo != 25)
            throw new UnexpectedCborException.UnexpectedType(CborType.Float16.name(), tokenType());
        return (short) tokenArg;
    }

    public float getFloat32() throws UnexpectedCborException {
        if (tokenMajorType != 7 || tokenAdditionalInfo != 26)
            throw new UnexpectedCborException.UnexpectedType(CborType.Float32.name(), tokenType());
        return Float.intBitsToFloat((int) tokenArg);
    }

    public double getFloat64() throws UnexpectedCborException {
        if (tokenMajorType != 7 || tokenAdditionalInfo != 27)
            throw new UnexpectedCborException.UnexpectedType(CborType.Float64.name(), tokenType());
        return Double.longBitsToDouble(tokenArg);
    }

    /** this should always follow a call to [nextToken] */
    public long getTag() throws UnexpectedCborException {
        if (tokenMajorType != 6)
            throw new UnexpectedCborException.UnexpectedType(CborType.Tagged.name(), tokenType());
        return tokenArg;
    }

    public boolean getBool() throws UnexpectedCborException {
        if (tokenMajorType != 7 || tokenAdditionalInfo > 24 || !(tokenArg == 20 || tokenArg == 21))
            throw new UnexpectedCborException.UnexpectedType(CborType.True.name() + " or " + CborType.False.name(), tokenType());
        return tokenArg == 21;
    }

    /**
     * this should always follow a call to [nextToken]
     * expects simple value = 23
     */
    public void getUndefined() throws UnexpectedCborException {
        if (tokenMajorType != 7 || tokenAdditionalInfo > 24 || tokenArg != 23)
            throw new UnexpectedCborException.UnexpectedType(CborType.Undefined.name(), tokenType());
    }

    /**
     * this should always follow a call to [nextToken]
     * expects simple value = 22
     */
    public void getNull() throws UnexpectedCborException {
        if (tokenMajorType != 7 || tokenAdditionalInfo > 24 || tokenArg != 22)
            throw new UnexpectedCborException.UnexpectedType(CborType.Undefined.name(), tokenType());
    }

    /**
     * this should always follow a call to [nextToken]
     * @return the known length of the array, or [Long.MIN_VALUE] if it's an indefinite length array (terminated by break)
     */
    public long getArray() throws UnexpectedCborException {
        if (tokenMajorType != 4)
            throw new UnexpectedCborException.UnexpectedType(CborType.Array.name(), tokenType());
        if (tokenIndefiniteLength)
            return Long.MIN_VALUE;
        return tokenArg;
    }

    /**
     * this should always follow a call to [nextToken]
     * @return the known number of pairs in the map, or [Long.MIN_VALUE] if it's an indefinite length map (terminated by break)
     */
    public long getMap() throws UnexpectedCborException {
        if (tokenMajorType != 5)
            throw new UnexpectedCborException.UnexpectedType(CborType.Map.name(), tokenType());
        if (tokenIndefiniteLength)
            return Long.MIN_VALUE;
        return tokenArg;
    }

    private final IndefiniteByteArrayReader indefiniteByteArrayReader = new IndefiniteByteArrayReader();
    private final FiniteByteArrayReader finiteByteArrayReader = new FiniteByteArrayReader();

    /**
     * this should always follow a call to [nextToken]
     */
    public ByteArrayReader getBytes() throws UnexpectedCborException {
        if (tokenMajorType != 2)
            throw new UnexpectedCborException.UnexpectedType(CborType.Bytes.name(), tokenType());
        if (tokenIndefiniteLength) {
            indefiniteByteArrayReader.init(2, CborType.Bytes.name());
            return indefiniteByteArrayReader;
        }
        finiteByteArrayReader.init(tokenArg);
        return finiteByteArrayReader;
    }

    private final ByteArrayReaderInputStream byteArrayReaderInputStream = new ByteArrayReaderInputStream();

    /**
     * this should always follow a call to [nextToken]
     */
    public Reader getUtf8() throws UnexpectedCborException {
        if (tokenMajorType != 3)
            throw new UnexpectedCborException.UnexpectedType(CborType.Utf8String.name(), tokenType());

        ByteArrayReader byteReader;
        if (tokenIndefiniteLength) {
            indefiniteByteArrayReader.init(3, CborType.Utf8String.name());
            byteReader = indefiniteByteArrayReader;
        } else {
            finiteByteArrayReader.init(tokenArg);
            byteReader = finiteByteArrayReader;
        }

        return new InputStreamReader(byteReader.inputStream(), StandardCharsets.UTF_8);
    }

    public final static class ByteArrayReaderInputStream extends InputStream {
        private ByteArrayReader reader;

        private void init(@NotNull ByteArrayReader reader) {
            this.reader = reader;
        }

        private ByteArrayReaderInputStream() {}

        @Override
        public int read() {
            if (!reader.hasNext())
                return -1;
            return reader.nextByte() & 0xFF;
        }

        @Override
        public int read(byte @NotNull [] b) {
            return read(b, 0, b.length);
        }

        @Override
        public int read(byte @NotNull [] b, int off, int len) {
            if (!reader.hasNext())
                return -1;
            return reader.next(b, off, len);
        }

        @Override
        public int available() {
            return reader.guessRemainingLength();
        }
    }

    public sealed abstract class ByteArrayReader implements Iterator<Byte> permits FiniteByteArrayReader, IndefiniteByteArrayReader {
        @Override
        public final Byte next() {
            return nextByte();
        }

        public abstract byte nextByte();

        /** reads up to [length] next bytes into [dst] at [offset], and returns how many bytes have been read */
        public abstract int next(byte[] dst, int offset, int length);

        /** reads up to [length] next bytes into [dst] */
        public final int next(byte[] dst) {
            return next(dst, 0, dst.length);
        }

        /** this will only return 0 if the array is at the end! otherwise will always return at least 1 */
        public int guessRemainingLength() {
            return 1;
        }

        public void skipToEnd() {
            byte[] buf = new byte[512];
            while (hasNext()) {
                next(buf);
            }
        }

        public ByteBuffer readAll(ByteBuffer dst) {
            byte[] buf = new byte[512];
            while (hasNext()) {
                dst.put(buf, 0, next(buf));
            }
            return dst;
        }

        public void readAll(OutputStream dst) throws IOException {
            byte[] buf = new byte[512];
            while (hasNext()) {
                dst.write(buf, 0, next(buf));
            }
        }

        public byte[] readAll() {
            var out = new ByteArrayOutputStream();
            byte[] buf = new byte[512];
            while (hasNext()) {
                out.write(buf, 0, next(buf));
            }
            return out.toByteArray();
        }

        public final ByteArrayReaderInputStream inputStream() {
            byteArrayReaderInputStream.init(this);
            return byteArrayReaderInputStream;
        }
    }

    private final class FiniteByteArrayReader extends ByteArrayReader {
        private long remaining;

        public void init(long length) {
            remaining = length;
        }

        @Override
        public boolean hasNext() {
            return remaining != 0;
        }

        @Override
        public byte nextByte() {
            if (remaining == 0)
                throw new NoSuchElementException();
            remaining -= 1;
            return buffer.get();
        }

        @Override
        public int next(byte[] dst, int offset, int length) {
            if (!hasNext())
                return 0;
            int read = length;
            if (read > remaining)
                read = guessRemainingLength();
            remaining -= read;
            buffer.get(dst, offset, read);
            return read;
        }

        @Override
        public int guessRemainingLength() {
            if (remaining < 0 || remaining > Integer.MAX_VALUE)
                return Integer.MAX_VALUE;
            return (int) remaining;
        }
    }

    private final class IndefiniteByteArrayReader extends ByteArrayReader {
        private boolean _hasFinite;
        private int majorType;
        @Nullable private String expectedName;

        public void init(int majorType, @Nullable String expectedName) {
            _hasFinite = false;
            this.majorType = majorType;
            this.expectedName = expectedName;
        }

        private @Nullable FiniteByteArrayReader chunkReader() {
            if (_hasFinite && finiteByteArrayReader.hasNext())
                return finiteByteArrayReader;
            _hasFinite = false;
            if (isBreak())
                return null;
            nextToken();
            if (isBreak())
                return null;
            if (tokenMajorType != majorType || tokenIndefiniteLength)
                throw new UnexpectedCborException.UnexpectedType(expectedName, tokenType());
            finiteByteArrayReader.init(tokenArg);
            _hasFinite = true;
            return finiteByteArrayReader;
        }

        @Override
        public byte nextByte() {
            var chunk = chunkReader();
            if (chunk == null)
                throw new NoSuchElementException();
            return chunk.nextByte();
        }

        @Override
        public int next(byte[] dst, int offset, int length) {
            var chunk = chunkReader();
            if (chunk == null)
                return 0;
            return chunk.next(dst, offset, length);
        }

        @Override
        public boolean hasNext() {
            return chunkReader() != null;
        }
    }

    /**
     * this should always follow a call to [nextToken].
     * reads the whole array, including the Break, if indefinite
     */
    public <T> @NotNull Iterator<T> readArray(@NotNull CborItemDecoder<T> elementDecoder) throws UnexpectedCborException {
        long length = getArray();
        if (length == Long.MIN_VALUE) {
            nextToken();
            return new IndefiniteLengthArrayIterator<>(this, elementDecoder);
        }
        return new FiniteLengthArrayIterator<>(this, elementDecoder, length);
    }

    /**
     * this should always follow a call to [nextToken].
     * reads the whole map, including the Break, if indefinite
     */
    public <K, V, T> @NotNull Iterator<T> readMap(@NotNull CborItemDecoder<K> keyDecoder,
                                                  @NotNull CborItemDecoder<V> valDecoder,
                                                  @NotNull BiFunction<K, V, T> makeItem) throws UnexpectedCborException {
        long length = getMap();
        if (length == Long.MIN_VALUE) {
            nextToken();
            return new IndefiniteLengthMapIterator<>(this, keyDecoder, valDecoder, makeItem);
        }
        return new FiniteLengthMapIterator<>(this, keyDecoder, valDecoder, makeItem, length);
    }

    /**
     * this should always follow a call to [nextToken].
     * reads the whole map, including the Break, if indefinite
     */
    public <K, V> void readMap(@NotNull CborItemDecoder<K> keyDecoder,
                               @NotNull CborItemDecoder<V> valDecoder,
                               @NotNull BiConsumer<K, V> each) throws UnexpectedCborException {

        var iter = readMap(keyDecoder, valDecoder, (a, b) -> {
            each.accept(a, b);
            return null;
        });
        while (iter.hasNext())
            iter.next();
    }

    private record IndefiniteLengthArrayIterator<T>(@NotNull CborDecoder decoder,
                                                    @NotNull CborItemDecoder<T> elementDecoder)
            implements Iterator<T>
    {
        @Override
        public boolean hasNext() {
            return !decoder.isBreak();
        }

        @Override
        public T next() {
            if (decoder.isBreak())
                throw new NoSuchElementException();
            var x = elementDecoder.next(decoder);
            decoder.nextToken();
            return x;
        }
    }

    private static class FiniteLengthArrayIterator<T> implements Iterator<T>
    {
        @NotNull private final CborDecoder decoder;
        @NotNull private final CborItemDecoder<T> elementDecoder;
        private long remaining;

        public FiniteLengthArrayIterator(@NotNull CborDecoder decoder,
                                         @NotNull CborItemDecoder<T> elementDecoder,
                                         long length) {
            this.decoder = decoder;
            this.elementDecoder = elementDecoder;
            this.remaining = length;
        }

        @Override
        public boolean hasNext() {
            return remaining > 0;
        }

        @Override
        public T next() {
            if (remaining <= 0)
                throw new NoSuchElementException();
            decoder.nextToken();
            remaining -= 1;
            return elementDecoder.next(decoder);
        }
    }

    private record IndefiniteLengthMapIterator<K, V, T>(
            @NotNull CborDecoder decoder,
            @NotNull CborItemDecoder<K> keyDecoder,
            @NotNull CborItemDecoder<V> valDecoder,
            @NotNull BiFunction<K, V, T> makeItem
    ) implements Iterator<T>
    {
        @Override
        public boolean hasNext() {
            return !decoder.isBreak();
        }

        @Override
        public T next() {
            if (decoder.isBreak())
                throw new NoSuchElementException();
            K k = keyDecoder.next(decoder);
            decoder.nextToken();
            if (decoder.isBreak())
                throw new InvalidCborException();
            V v = valDecoder.next(decoder);
            decoder.nextToken();
            return makeItem.apply(k, v);
        }
    }

    private static class FiniteLengthMapIterator<K, V, T> implements Iterator<T>
    {
        @NotNull private final CborDecoder decoder;
        @NotNull private final CborItemDecoder<K> keyDecoder;
        @NotNull private final CborItemDecoder<V> valDecoder;
        @NotNull private final BiFunction<K, V, T> makeItem;
        private long remaining;

        public FiniteLengthMapIterator(@NotNull CborDecoder decoder,
                                       @NotNull CborItemDecoder<K> keyDecoder,
                                       @NotNull CborItemDecoder<V> valDecoder,
                                       @NotNull BiFunction<K, V, T> makeItem,
                                       long length) {
            this.decoder = decoder;
            this.keyDecoder = keyDecoder;
            this.valDecoder = valDecoder;
            this.makeItem = makeItem;
            this.remaining = length;
        }

        @Override
        public boolean hasNext() {
            return remaining > 0;
        }

        @Override
        public T next() {
            if (remaining <= 0)
                throw new NoSuchElementException();
            remaining -= 1;
            decoder.nextToken();
            K k = keyDecoder.next(decoder);
            decoder.nextToken();
            V v = valDecoder.next(decoder);
            return makeItem.apply(k, v);
        }
    }
}
