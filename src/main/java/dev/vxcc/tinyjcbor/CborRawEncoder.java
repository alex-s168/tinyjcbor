package dev.vxcc.tinyjcbor;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Only use this if you know the details of CBOR.
 *
 * @see CborEncoder
 *
 * @since 1.0.0-rc.1
 */
public final class CborRawEncoder {
    @NotNull
    public final OutputStream out;
    @NotNull
    private final ByteBuffer _temp;
    private final byte @NotNull [] _buf8;

    public CborRawEncoder(@NotNull ByteOrder byteOrder, @NotNull OutputStream out) {
        this.out = out;

        this._temp = ByteBuffer.allocate(8);
        this._temp.order(byteOrder);
        this._buf8 = new byte[8];
    }

    private void writeShort(short s) throws IOException {
        _temp.clear();
        _temp.putShort(s);
        _temp.flip();
        _temp.get(_buf8, 0, 2);
        out.write(_buf8, 0, 2);
    }

    private void writeInt(int i) throws IOException {
        _temp.clear();
        _temp.putInt(i);
        _temp.flip();
        _temp.get(_buf8, 0, 4);
        out.write(_buf8, 0, 4);
    }

    private void writeLong(long l) throws IOException {
        _temp.clear();
        _temp.putLong(l);
        _temp.flip();
        _temp.get(_buf8, 0, 8);
        out.write(_buf8, 0, 8);
    }

    private void writeTokenHeader(int major, int additional) throws IOException {
        if (major > 0b111 || major < 0 || additional < 0 || additional > 0b11111)
            throw new IllegalArgumentException();
        out.write((major << 5) | additional);
    }

    private void writeTokenWithArg(int major, long arg) throws IOException {
        if (arg < 0 || arg > Integer.MAX_VALUE) {
            writeTokenHeader(major, 27);
            writeLong(arg);
        }
        else if (arg > Short.MAX_VALUE) {
            writeTokenHeader(major, 26);
            writeInt((int) arg);
        }
        else if (arg > Byte.MAX_VALUE) {
            writeTokenHeader(major, 25);
            writeShort((short) arg);
        }
        else if (arg > 24) {
            writeTokenHeader(major, 24);
            out.write((int) arg);
        } else {
            writeTokenHeader(major, (int) arg);
        }
    }

    public void writeBreak() throws IOException {
        writeTokenHeader(7, 31);
    }

    public void writeBeginIndefinite(long major) throws IOException {
        if (major < 2 || major > 5)
            throw new IllegalArgumentException();
        writeTokenHeader((int) major, 31);
    }

    public void writeBeginFinite(int major, long len) throws IOException {
        if (major < 2 || major > 5)
            throw new IllegalArgumentException();
        writeTokenWithArg(major, len);
    }

    public void writeTag(long tag) throws IOException {
        writeTokenWithArg(6, tag);
    }

    public void writeSimple(byte x) throws IOException {
        if (x < 24) {
            writeTokenHeader(7, x);
        } else {
            writeTokenHeader(7, 24);
            out.write((int) x);
        }
    }

    public void writeF16(short x) throws IOException {
        writeTokenHeader(7, 25);
        writeShort(x);
    }

    public void writeF32(int x) throws IOException {
        writeTokenHeader(7, 26);
        writeInt(x);
    }

    public void writeF64(long x) throws IOException {
        writeTokenHeader(7, 27);
        writeLong(x);
    }

    public void writeUInt(long x) throws IOException {
        writeTokenWithArg(0, x);
    }

    public void writeSInt(long x) throws IOException {
        if (x >= 0) {
            writeUInt(x);
        } else {
            writeTokenWithArg(1, -x - 1);
        }
    }
}
