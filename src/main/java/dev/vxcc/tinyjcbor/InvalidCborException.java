package dev.vxcc.tinyjcbor;

public class InvalidCborException extends RuntimeException {
    @Override
    public String toString() {
        return "The CBOR given into the decoder is not valid according to RFC8949";
    }
}
