package dev.vxcc.tinyjcbor;

import dev.vxcc.tinyjcbor.serde.CborDeserializer;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * Standard CBOR decoder
 * <p>Uses the buffer's byte order as network order for integers & floats
 *
 * @since 1.0.0-rc.1
 */
public final class CborDecoder {
    @NotNull
    private final ByteBuffer buffer;

    private long tokenArg;
    private int tokenMajorType = 69;
    private int tokenAdditionalInfo;
    private boolean tokenIndefiniteLength;

    /**
     * Construct a new CBOR decoder, with the byte order of the buffer!
     *
     * @param buffer byte buffer from which CBOR is decoded from.
     *               <br>You can continue to use the buffer after you finished decoding CBOR, to for example read additional data, or to make sure all data has been decoded.
     */
    public CborDecoder(@NotNull ByteBuffer buffer) {
        this.buffer = buffer;
    }

    /**
     * Represents a copy of the state of a CBOR decoder at a past point in time.
     *
     * @see #reset(Snapshot)
     * @see #snapshot()
     */
    public static final class Snapshot {
        private int position;
        private long tokenArg;
        private int tokenMajorType;
        private int tokenAdditionalInfo;
        private boolean tokenIndefiniteLength;

        /** Back-up the given decoder into this snapshot */
        public void from(@NotNull CborDecoder decoder) {
            position = decoder.buffer.position();
            tokenArg = decoder.tokenArg;
            tokenMajorType = decoder.tokenMajorType;
            tokenAdditionalInfo = decoder.tokenAdditionalInfo;
            tokenIndefiniteLength = decoder.tokenIndefiniteLength;
        }
    }

    /**
     * Back-up the whole decoder state, so it can be set back to the snapshot again later.
     *
     * <p>If you do multiple snapshots, you should manually create an instance of {@code Snapshot}, and call {@code Snapshot#from}, like this:</p>
     *
     * <pre><code>
     *     var snap = new CborDecoder.Snapshot();
     *     snap.from(decoder);
     * </code></pre>
     *
     * @see #reset(Snapshot)
     * @see Snapshot#from(CborDecoder)
     */
    public @NotNull Snapshot snapshot() {
        var snap = new Snapshot();
        snap.from(this);
        return snap;
    }

    /** Load decoder state from snapshot / restore snapshot */
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

    private void nextToken() throws InvalidCborException {
        if (!hasNext())
            throw new NoSuchElementException();
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

    private @NotNull CborType currentTokenType() {
        return switch (tokenMajorType) {
            case 0 -> CborType.UnsignedInteger;
            case 1 -> CborType.NegativeInteger;
            case 2 -> CborType.ByteString;
            case 3 -> CborType.Text;
            case 4 -> CborType.Array;
            case 5 -> CborType.Map;
            case 6 -> CborType.Tag;
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

    @NotNull
    private final Snapshot peekSnapshot = new Snapshot();

    /**
     * returns the type of the next token, without reading it
     * @return null, if there is no next token<br>
     *         the type of the next token
     * @throws InvalidCborException data is not valid CBOR
     * @since 1.0.0-rc.1
     */
    public @Nullable CborType peekTokenType() {
        if (!hasNext())
            return null;
        peekSnapshot.from(this);
        nextToken();
        var ty = currentTokenType();
        reset(peekSnapshot);
        return ty;
    }

    /**
     * @throws NoSuchElementException there is no next item
     * @throws UnexpectedCborException CBOR data in buffer does not match expected schema
     * @throws InvalidCborException data is not valid CBOR
     * @since 1.0.0-rc.1
     */
    public <T> T read(@NotNull CborDeserializer<T> decoder) throws UnexpectedCborException {
        return decoder.next(this);
    }

    /**
     * Skip the whole next item. Also follows hierarchical structures (arrays, maps, tags)
     * @throws NoSuchElementException there is no next item
     * @throws InvalidCborException data is not valid CBOR
     * @since 1.0.0-rc.1
     */
    public void readAny()  {
        nextToken();
        try {
            switch (currentTokenType()) {
                case Tag:
                    readAny();
                    break;

                case Map:
                    if (tokenIndefiniteLength) {
                        while (peekTokenType() != CborType.Break) {
                            readAny();
                            readAny();
                        }
                        readBreak();
                    } else {
                        long n = tokenArg;
                        for (long i = 0; i < n; i++) {
                            readAny();
                            readAny();
                        }
                    }
                    break;

                case Array:
                    if (tokenIndefiniteLength) {
                        while (peekTokenType() != CborType.Break)
                            readAny();
                        readBreak();
                    } else {
                        long n = tokenArg;
                        for (long i = 0; i < n; i++)
                            readAny();
                    }
                    break;

                case Text:
                case ByteString:
                    if (tokenIndefiniteLength) {
                        while (peekTokenType() != CborType.Break)
                            readAny();
                        readBreak();
                    } else {
                        long n = tokenArg;
                        for (long i = 0; i < n; i++)
                            buffer.get();
                    }
                    break;
            }
        } catch (NoSuchElementException ignored) {
            throw new InvalidCborException();
        }
    }

    /**
     * @throws UnexpectedCborException next token is not a simple value
     * @throws NoSuchElementException there is no next item
     * @throws InvalidCborException data is not valid CBOR
     * @since 1.0.0-rc.1
     */
    public byte readSimple() throws UnexpectedCborException {
        nextToken();
        if (tokenMajorType != 7 || tokenAdditionalInfo > 24)
            throw new UnexpectedCborException.UnexpectedType(CborType.SimpleValue.name(), currentTokenType());
        return (byte) tokenArg;
    }

    /**
     * @throws UnexpectedCborException next token is not a unsigned integer
     * @throws NoSuchElementException there is no next item
     * @throws InvalidCborException data is not valid CBOR
     * @since 1.0.0-rc.1
     */
    public long readUInt() throws UnexpectedCborException {
        nextToken();
        if (tokenMajorType != 0)
            throw new UnexpectedCborException.UnexpectedType(CborType.UnsignedInteger.name(), currentTokenType());
        return tokenArg;
    }

    /**
     * Read a signed or unsigned integer.
     * @throws UnexpectedCborException next token is not a signed or unsigned integer
     * @throws NoSuchElementException there is no next item
     * @throws InvalidCborException data is not valid CBOR
     * @since 1.0.0-rc.1
     */
    public long readInt() throws UnexpectedCborException {
        nextToken();
        return switch (tokenMajorType) {
            case 0 -> tokenArg;
            case 1 -> -1 - tokenArg;
            default ->
                    throw new UnexpectedCborException.UnexpectedType(CborType.UnsignedInteger.name() + " or " + CborType.NegativeInteger.name(), currentTokenType());
        };
    }

    /**
     * @throws UnexpectedCborException next token is not a 16-bit float
     * @throws NoSuchElementException there is no next item
     * @throws InvalidCborException data is not valid CBOR
     * @since 1.0.0-rc.1
     */
    public short readFloat16() throws UnexpectedCborException {
        nextToken();
        if (tokenMajorType != 7 || tokenAdditionalInfo != 25)
            throw new UnexpectedCborException.UnexpectedType(CborType.Float16.name(), currentTokenType());
        return (short) tokenArg;
    }

    /**
     * @throws UnexpectedCborException next token is not a 32-bit float
     * @throws NoSuchElementException there is no next item
     * @throws InvalidCborException data is not valid CBOR
     * @since 1.0.0-rc.1
     */
    public float readFloat32() throws UnexpectedCborException {
        nextToken();
        if (tokenMajorType != 7 || tokenAdditionalInfo != 26)
            throw new UnexpectedCborException.UnexpectedType(CborType.Float32.name(), currentTokenType());
        return Float.intBitsToFloat((int) tokenArg);
    }

    /**
     * @throws UnexpectedCborException next token is not a 64-bit float
     * @throws NoSuchElementException there is no next item
     * @throws InvalidCborException data is not valid CBOR
     * @since 1.0.0-rc.1
     */
    public double readFloat64() throws UnexpectedCborException {
        nextToken();
        if (tokenMajorType != 7 || tokenAdditionalInfo != 27)
            throw new UnexpectedCborException.UnexpectedType(CborType.Float64.name(), currentTokenType());
        return Double.longBitsToDouble(tokenArg);
    }

    /**
     * @throws UnexpectedCborException next token is not a tag
     * @throws NoSuchElementException there is no next item
     * @throws InvalidCborException data is not valid CBOR
     * @since 1.0.0-rc.1
     */
    public long readTag() throws UnexpectedCborException {
        nextToken();
        if (tokenMajorType != 6)
            throw new UnexpectedCborException.UnexpectedType(CborType.Tag.name(), currentTokenType());
        return tokenArg;
    }

    /**
     * @throws UnexpectedCborException next token is not true or false
     * @throws NoSuchElementException there is no next item
     * @throws InvalidCborException data is not valid CBOR
     * @since 1.0.0-rc.1
     */
    public boolean readBool() throws UnexpectedCborException {
        nextToken();
        if (tokenMajorType != 7 || tokenAdditionalInfo > 24 || !(tokenArg == 20 || tokenArg == 21))
            throw new UnexpectedCborException.UnexpectedType(CborType.True.name() + " or " + CborType.False.name(), currentTokenType());
        return tokenArg == 21;
    }

    /**
     * simple value = 23
     * @throws UnexpectedCborException next token is not of type undefined
     * @throws NoSuchElementException there is no next item
     * @throws InvalidCborException data is not valid CBOR
     * @since 1.0.0-rc.1
     */
    public void readUndefined() throws UnexpectedCborException {
        nextToken();
        if (tokenMajorType != 7 || tokenAdditionalInfo > 24 || tokenArg != 23)
            throw new UnexpectedCborException.UnexpectedType(CborType.Undefined.name(), currentTokenType());
    }

    /**
     * simple value = 22
     * @throws UnexpectedCborException next token is not of type null
     * @throws NoSuchElementException there is no next item
     * @throws InvalidCborException data is not valid CBOR
     * @since 1.0.0-rc.1
     */
    public void readNull() throws UnexpectedCborException {
        nextToken();
        if (tokenMajorType != 7 || tokenAdditionalInfo > 24 || tokenArg != 22)
            throw new UnexpectedCborException.UnexpectedType(CborType.Undefined.name(), currentTokenType());
    }

    /**
     * @throws UnexpectedCborException next token is not of type break
     * @throws NoSuchElementException there is no next item
     * @throws InvalidCborException data is not valid CBOR
     */
    private void readBreak() throws UnexpectedCborException {
        nextToken();
        if (currentTokenType() != CborType.Break)
            throw new UnexpectedCborException.UnexpectedType(CborType.Break.name(), currentTokenType());
    }

    /**
     * @return the known length of the array<br>
     *         or {@code Long.MIN_VALUE} if it's an indefinite length array (terminated by break)
     *
     * @see #readArray(CborDeserializer)
     * @see #readArrayManual()
     *
     * @throws UnexpectedCborException next token is not an array
     * @throws NoSuchElementException there is no next item
     * @throws InvalidCborException data is not valid CBOR
     */
    private long readArrayRaw() throws UnexpectedCborException {
        nextToken();
        if (tokenMajorType != 4)
            throw new UnexpectedCborException.UnexpectedType(CborType.Array.name(), currentTokenType());
        if (tokenIndefiniteLength)
            return Long.MIN_VALUE;
        return tokenArg;
    }

    /**
     * @return the known number of pairs in the map,<br>
     *         or {@code Long.MIN_VALUE} if it's an indefinite length map (terminated by break)
     *
     * @see #readMap(CborDeserializer, CborDeserializer, BiFunction)
     * @see #readMapManual()
     *
     * @throws UnexpectedCborException next token is not a map
     * @throws NoSuchElementException there is no next item
     * @throws InvalidCborException data is not valid CBOR
     */
    private long readMapRaw() throws UnexpectedCborException {
        nextToken();
        if (tokenMajorType != 5)
            throw new UnexpectedCborException.UnexpectedType(CborType.Map.name(), currentTokenType());
        if (tokenIndefiniteLength)
            return Long.MIN_VALUE;
        return tokenArg;
    }

    private final IndefiniteByteReader indefiniteByteArrayReader = new IndefiniteByteReader();
    private final FiniteByteReader finiteByteArrayReader = new FiniteByteReader();

    /**
     * Read a byte string (byte array)
     * <p>All items must be consumed!
     * <br><br>
     *
     * Example:
     * <pre><code>
     *     var reader = decoder.readByteString();
     *     var buf = new byte[512];
     *     while (reader.hasNext()) {
     *         var num = reader.read(buf);
     *         ...
     *     }
     * </code></pre>
     * <br>
     *
     * It can also be converted to an input stream:
     * <pre><code>
     *     var reader = decoder.readByteString();
     *     reader.inputStream().transferTo(somewhere);
     *     reader.skipToEnd(); // make sure all bytes have been read
     * </code></pre>
     * <br>
     *
     * But it is also an Iterator, so you can do:
     * <pre><code>
     *     var items = new OnceIterable(decoder.readByteString());
     *     for (byte b : items) {
     *         ...
     *     }
     * </code></pre>
     *
     * @see dev.vxcc.tinyjcbor.serde.CborPrim#BYTES Deserializer for directly reading a {@code byte[]}
     *
     * @throws UnexpectedCborException next token is not a byte string
     * @throws NoSuchElementException there is no next item
     * @throws InvalidCborException data is not valid CBOR
     *
     * @since 1.0.0-rc.1
     */
    @CheckReturnValue
    public ByteReader readByteString() throws UnexpectedCborException {
        nextToken();
        if (tokenMajorType != 2)
            throw new UnexpectedCborException.UnexpectedType(CborType.ByteString.name(), currentTokenType());
        if (tokenIndefiniteLength) {
            indefiniteByteArrayReader.init(2, CborType.ByteString.name());
            return indefiniteByteArrayReader;
        }
        finiteByteArrayReader.init(tokenArg);
        return finiteByteArrayReader;
    }

    private final ByteReaderInputStream byteReaderInputStream = new ByteReaderInputStream();

    /**
     * Read a text (utf8 string) as chars
     * <p>All items must be consumed!
     * <br><br>
     *
     * Example:
     * <pre><code>
     *     var reader = decoder.readText();
     *     var out = new StringWriter();
     *     reader.transferTo(out);
     *     String read = out.toString();
     * </code></pre>
     * <br>
     *
     * @see dev.vxcc.tinyjcbor.serde.CborPrim#STRING Deserializer for directly reading a {@code String}
     * @see #readTextUtf8()
     *
     * @throws UnexpectedCborException next token is not text (utf8 string)
     * @throws NoSuchElementException there is no next item
     * @throws InvalidCborException data is not valid CBOR
     *
     * @since 1.0.0-rc.1
     */
    @CheckReturnValue
    public Reader readText() throws UnexpectedCborException {
        nextToken();
        if (tokenMajorType != 3)
            throw new UnexpectedCborException.UnexpectedType(CborType.Text.name(), currentTokenType());

        ByteReader byteReader;
        if (tokenIndefiniteLength) {
            indefiniteByteArrayReader.init(3, CborType.Text.name());
            byteReader = indefiniteByteArrayReader;
        } else {
            finiteByteArrayReader.init(tokenArg);
            byteReader = finiteByteArrayReader;
        }

        return new InputStreamReader(byteReader.inputStream(), StandardCharsets.UTF_8);
    }

    /**
     * Read a text (utf8 string) as utf8 bytes.
     * <p>All items must be consumed!
     * <br><br>
     *
     * Example:
     * <pre><code>
     *     var reader = decoder.readTextUtf8();
     *     var buf = new byte[512];
     *     while (reader.hasNext()) {
     *         var num = reader.read(buf);
     *         ...
     *     }
     * </code></pre>
     * <br>
     *
     * It can also be converted to an input stream:
     * <pre><code>
     *     var reader = decoder.readTextUtf8();
     *     reader.inputStream().transferTo(somewhere);
     *     reader.skipToEnd(); // make sure all bytes have been read
     * </code></pre>
     * <br>
     *
     * But it is also an Iterator, so you can do:
     * <pre><code>
     *     var items = new OnceIterable(decoder.readTextUtf8());
     *     for (byte b : items) {
     *         ...
     *     }
     * </code></pre>
     *
     * @see dev.vxcc.tinyjcbor.serde.CborPrim#STRING Decoder for directly reading a String
     * @see #readText()
     *
     * @throws UnexpectedCborException next token is not text (utf8 string)
     * @throws NoSuchElementException there is no next item
     * @throws InvalidCborException data is not valid CBOR
     *
     * @since 1.0.0-rc.2
     */
    @CheckReturnValue
    public ByteReader readTextUtf8() throws UnexpectedCborException {
        nextToken();
        if (tokenMajorType != 3)
            throw new UnexpectedCborException.UnexpectedType(CborType.Text.name(), currentTokenType());

        if (tokenIndefiniteLength) {
            indefiniteByteArrayReader.init(3, CborType.Text.name());
            return indefiniteByteArrayReader;
        } else {
            finiteByteArrayReader.init(tokenArg);
            return finiteByteArrayReader;
        }
    }

    /**
     * A {@code InputStream} wrapper for {@code ByteReader}
     *
     * Functions in here can throw {@code InvalidCborException}
     *
     * @see ByteReader#inputStream()
     *
     * @since 1.0.0-rc.1
     */
    public final static class ByteReaderInputStream extends InputStream {
        private ByteReader reader;

        private void init(@NotNull CborDecoder.ByteReader reader) {
            this.reader = reader;
        }

        private ByteReaderInputStream() {}

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

    /**
     * A reader for reading byte strings and text.
     *
     * <p>Can be converted to a {@code InputStream} using {@code #inputStream()}
     * <p>All available bytes must be read!
     * <p>Do not keep references to this when you call the next function on CborDecoder
     *
     * @see #readByteString()
     * @see #readText()
     *
     * @since 1.0.0-rc.1
     */
    public sealed abstract class ByteReader implements Iterator<Byte> permits FiniteByteReader, IndefiniteByteReader {
        /**
         * For better performance, use {@code #nextByte()} instead
         * @see #nextByte()
         * @throws NoSuchElementException when there is no next byte
         * @throws InvalidCborException data is not valid CBOR
         * @since 1.0.0-rc.1
         */
        @Override
        public final Byte next() {
            return nextByte();
        }

        /**
         * @throws NoSuchElementException when there is no next byte
         * @throws InvalidCborException data is not valid CBOR
         * @since 1.0.0-rc.1
         */
        public abstract byte nextByte();

        /**
         * reads up to [length] next bytes into [dst] at [offset], and returns how many bytes have been read
         * @return how many bytes were read
         * @throws InvalidCborException data is not valid CBOR
         * @since 1.0.0-rc.1
         */
        @CheckReturnValue
        public abstract int next(byte[] dst, int offset, int length);

        /**
         * reads up to [length] next bytes into [dst]
         * @return how many bytes were read
         * @throws InvalidCborException data is not valid CBOR
         * @since 1.0.0-rc.1
         */
        @CheckReturnValue
        public final int next(byte[] dst) {
            return next(dst, 0, dst.length);
        }

        /**
         * @return 0 if the array is at the end, otherwise at least 1
         * @throws InvalidCborException data is not valid CBOR
         * @since 1.0.0-rc.1
         */
        public int guessRemainingLength() {
            return 1;
        }

        /**
         * Read all remaining bytes
         * @throws InvalidCborException data is not valid CBOR
         * @since 1.0.0-rc.1
         */
        public void skipToEnd() {
            byte[] buf = new byte[512];
            //noinspection StatementWithEmptyBody
            while ((next(buf)) != 0);
        }

        /**
         * Read all remaining bytes into the destination buffer
         * @see #readAll(ByteBuffer)
         * @see #readAll()
         * @throws InvalidCborException data is not valid CBOR
         * @since 1.0.0-rc.1
         */
        public ByteBuffer readAll(ByteBuffer dst) {
            byte[] buf = new byte[512];
            while (hasNext()) {
                dst.put(buf, 0, next(buf));
            }
            return dst;
        }

        /**
         * Read all remaining bytes into the given output stream
         * @see #readAll(ByteBuffer)
         * @see #readAll()
         * @throws InvalidCborException data is not valid CBOR
         * @since 1.0.0-rc.1
         */
        public void readAll(OutputStream dst) throws IOException {
            byte[] buf = new byte[512];
            while (hasNext()) {
                dst.write(buf, 0, next(buf));
            }
        }

        /**
         * Read all remaining bytes into an array
         * @see #readAll(ByteBuffer)
         * @see #readAll()
         * @throws InvalidCborException data is not valid CBOR
         * @since 1.0.0-rc.1
         */
        public byte[] readAll() {
            var out = new ByteArrayOutputStream();
            byte[] buf = new byte[512];
            while (hasNext()) {
                out.write(buf, 0, next(buf));
            }
            return out.toByteArray();
        }

        /**
         * Create an input stream wrapping this reader
         * @since 1.0.0-rc.1
         */
        @CheckReturnValue
        public final ByteReaderInputStream inputStream() {
            byteReaderInputStream.init(this);
            return byteReaderInputStream;
        }
    }

    private final class FiniteByteReader extends ByteReader {
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

    private final class IndefiniteByteReader extends ByteReader {
        private boolean _hasFinite;
        private int majorType;
        @Nullable private String expectedName;
        private boolean _end;

        public void init(int majorType, @Nullable String expectedName) {
            _hasFinite = false;
            this.majorType = majorType;
            this.expectedName = expectedName;
            this._end = false;
        }

        private @Nullable CborDecoder.FiniteByteReader chunkReader() {
            if (_end)
                return null;
            if (_hasFinite && finiteByteArrayReader.hasNext())
                return finiteByteArrayReader;
            _hasFinite = false;

            if (peekTokenType() == CborType.Break) {
                nextToken();
                _end = true;
                return null;
            }

            nextToken();

            if (tokenMajorType != majorType || tokenIndefiniteLength)
                throw new UnexpectedCborException.UnexpectedType(expectedName, currentTokenType());
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

    @NotNull
    private final ManualReader manualReader = new ManualReader();

    /**
     * Helper for manually reading an array.
     *
     * <pre><code>
     *     var arr = decoder.readArrayManual();
     *     arr.next(); var a = decoder.readInt();
     *     arr.next(); var b = decoder.readInt();
     *     arr.end();
     * </code></pre>
     *
     * @see #readArray(CborDeserializer)
     *
     * @throws UnexpectedCborException next token is not an array
     * @throws NoSuchElementException there is no next item
     * @throws InvalidCborException data is not valid CBOR
     *
     * @since 1.0.0-rc.1
     */
    @CheckReturnValue
    public @NotNull ManualReader readArrayManual() throws UnexpectedCborException {
        long length = readArrayRaw();
        manualReader.init(length);
        return manualReader;
    }

    /**
     * Helper for manually reading a map.
     *
     * <pre><code>
     *     var map = decoder.readMapManual();
     *     map.next();
     *     var k1 = decoder.readInt();
     *     var v1 = decoder.readInt();
     *     arr.next();
     *     var k2 = decoder.readInt();
     *     var v2 = decoder.readInt();
     *     // assert that the map ends here
     *     arr.end();
     * </code></pre>
     *
     * Example: efficiently read specific keys from map, ignoring unknown keys:
     * <pre><code>
     *     String name;
     *     String password;
     *
     *     var map = decoder.readMapManual();
     *     while (map.hasNext()) {
     *         map.next();
     *         if (decoder.peekTokenType() != CborType.Text) {
     *             decoder.readAny();
     *             decoder.readAny();
     *             continue;
     *         }
     *
     *         switch (decoder.read(CborPrim.STRING)) {
     *             case "name": name = decoder.read(CborPrim.STRING); break;
     *             case "password": password = decoder.read(CborPrim.STRING); break;
     *             default: break;
     *         }
     *     }
     *     map.end();
     * </code></pre>
     *
     * @see #readMap(CborDeserializer, CborDeserializer, BiFunction)
     *
     * @throws UnexpectedCborException next token is not a map
     * @throws NoSuchElementException there is no next item
     * @throws InvalidCborException data is not valid CBOR
     *
     * @since 1.0.0-rc.1
     */
    @CheckReturnValue
    public @NotNull ManualReader readMapManual() throws UnexpectedCborException {
        long length = readMapRaw();
        manualReader.init(length);
        return manualReader;
    }

    /**
     * Helper for manually reading a map or an array.
     * <p>If you are reading a map, only call {@code next()} once per key-value pair.
     *
     * <pre><code>
     *     var arr = decoder.readArrayManual();
     *     arr.next(); var a = decoder.readInt();
     *     arr.next(); var b = decoder.readInt();
     *     // assert that the map ends here
     *     arr.end();
     * </code></pre>
     *
     * @see #readArrayManual()
     * @see #readMapManual()
     *
     * @since 1.0.0-rc.1
     */
    public class ManualReader {
        private long length;
        private long i;
        private boolean end = true;
        private int lastBufPos;

        /**
         * @param length array/map num items, or Long.MIN_VALUE
         */
        private void init(long length) {
            if (!end)
                throw new IllegalStateException("Forgot to call ManualReader#end");
            this.end = false;
            this.length = length;
            this.i = 0;
            this.lastBufPos = buffer.position() - 1;
        }

        private ManualReader() {}

        public boolean hasNext() {
            if (length == Long.MIN_VALUE)
                return peekTokenType() != CborType.Break;
            else
                return length > 0;
        }

        public void next() throws UnexpectedCborException {
            if (lastBufPos == buffer.position())
                throw new IllegalStateException("Caller of ManualReader#next didn't read elements between two next() calls");
            lastBufPos = buffer.position();

            if (!hasNext())
                throw new UnexpectedCborException.UnexpectedEndOfArray(i);
            i++;
            if (length != Long.MIN_VALUE)
                length--;
        }

        public void end() throws UnexpectedCborException {
            if (hasNext())
                throw new UnexpectedCborException.ExpectedEndOfArray(i);
            if (length == Long.MIN_VALUE)
                readBreak();
        }
    }

    /**
     * Read an array
     * <p>All items in the returned iterator must be consumed!
     *
     * @throws UnexpectedCborException next token is not an array
     * @throws NoSuchElementException there is no next item
     * @throws InvalidCborException data is not valid CBOR
     *
     * @see #readArrayManual()
     *
     * @since 1.0.0-rc.1
     */
    @CheckReturnValue
    public <T> @NotNull Iterator<T> readArray(@NotNull CborDeserializer<T> elementDecoder) throws UnexpectedCborException {
        long length = readArrayRaw();
        if (length == Long.MIN_VALUE) {
            return new IndefiniteLengthArrayIterator<>(elementDecoder);
        }
        return new FiniteLengthArrayIterator<>(elementDecoder, length);
    }

    /**
     * Reads a map
     * <p>All items in the returned iterator must be consumed!
     *
     * @throws UnexpectedCborException next token is not a map
     * @throws NoSuchElementException there is no next item
     * @throws InvalidCborException data is not valid CBOR
     *
     * @see #readMapManual()
     *
     * @since 1.0.0-rc.1
     */
    @CheckReturnValue
    public <K, V, T> @NotNull Iterator<T> readMap(@NotNull CborDeserializer<K> keyDecoder,
                                                  @NotNull CborDeserializer<V> valDecoder,
                                                  @NotNull BiFunction<K, V, T> makeItem) throws UnexpectedCborException {
        long length = readMapRaw();
        if (length == Long.MIN_VALUE) {
            return new IndefiniteLengthMapIterator<>(keyDecoder, valDecoder, makeItem);
        }
        return new FiniteLengthMapIterator<>(keyDecoder, valDecoder, makeItem, length);
    }

    /**
     * Call a function for each element in the map
     *
     * @throws UnexpectedCborException next token is not a map
     * @throws NoSuchElementException there is no next item
     * @throws InvalidCborException data is not valid CBOR
     *
     * @see #readMapManual()
     *
     * @since 1.0.0-rc.1
     */
    public <K, V> void readMap(@NotNull CborDeserializer<K> keyDecoder,
                               @NotNull CborDeserializer<V> valDecoder,
                               @NotNull BiConsumer<K, V> each) throws UnexpectedCborException {

        var iter = readMap(keyDecoder, valDecoder, (a, b) -> {
            each.accept(a, b);
            return null;
        });
        while (iter.hasNext())
            iter.next();
    }

    private final class IndefiniteLengthArrayIterator<T> implements Iterator<T> {
        @NotNull private final CborDeserializer<T> elementDecoder;

        public IndefiniteLengthArrayIterator(@NotNull CborDeserializer<T> elementDecoder) {
            this.elementDecoder = elementDecoder;
        }

        @Override
        public boolean hasNext() {
            if (peekTokenType() == CborType.Break) {
                nextToken();
                return false;
            }
            return true;
        }

        @Override
        public T next() {
            if (peekTokenType() == CborType.Break)
                throw new NoSuchElementException();
            return elementDecoder.next(CborDecoder.this);
        }
    }

    private final class FiniteLengthArrayIterator<T> implements Iterator<T>
    {
        @NotNull private final CborDeserializer<T> elementDecoder;
        private long remaining;

        public FiniteLengthArrayIterator(@NotNull CborDeserializer<T> elementDecoder,
                                         long length) {
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
            remaining -= 1;
            return elementDecoder.next(CborDecoder.this);
        }
    }

    private final class IndefiniteLengthMapIterator<K, V, T> implements Iterator<T> {
        @NotNull private final CborDeserializer<K> keyDecoder;
        @NotNull private final CborDeserializer<V> valDecoder;
        @NotNull private final BiFunction<K, V, T> makeItem;

        public IndefiniteLengthMapIterator(@NotNull CborDeserializer<K> keyDecoder,
                                           @NotNull CborDeserializer<V> valDecoder,
                                           @NotNull BiFunction<K, V, T> makeItem) {
            this.keyDecoder = keyDecoder;
            this.valDecoder = valDecoder;
            this.makeItem = makeItem;
        }

        @Override
        public boolean hasNext() {
            if (peekTokenType() == CborType.Break) {
                nextToken(); // Consume the break token
                return false;
            }
            return true;
        }

        @Override
        public T next() {
            if (peekTokenType() == CborType.Break)
                throw new NoSuchElementException();

            K k = keyDecoder.next(CborDecoder.this);
            try {
                if (peekTokenType() == CborType.Break)
                    throw new InvalidCborException();
            } catch (InvalidCborException e) {
                throw new IllegalStateException("CBOR parsing error", e);
            }
            V v = valDecoder.next(CborDecoder.this);
            return makeItem.apply(k, v);
        }
    }

    private final class FiniteLengthMapIterator<K, V, T> implements Iterator<T> {
        @NotNull private final CborDeserializer<K> keyDecoder;
        @NotNull private final CborDeserializer<V> valDecoder;
        @NotNull private final BiFunction<K, V, T> makeItem;
        private long remaining;

        public FiniteLengthMapIterator(@NotNull CborDeserializer<K> keyDecoder,
                                       @NotNull CborDeserializer<V> valDecoder,
                                       @NotNull BiFunction<K, V, T> makeItem,
                                       long length) {
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
            K k = keyDecoder.next(CborDecoder.this);
            V v = valDecoder.next(CborDecoder.this);
            return makeItem.apply(k, v);
        }
    }
}
