package dev.vxcc.tinyjcbor;

/** CBOR Major Type */
public enum CborType {
    /** positive integer */
    UnsignedInteger,
    /** integer that is definitely negative */
    NegativeInteger,
    /** byte array */
    ByteString,
    /** UTF-8 encoded string */
    Text,
    /** Contains multiple CBOR items */
    Array,
    /** Contains multiple pairs of CBOR items */
    Map,
    /** a tag prefix of an item */
    Tag,
    /** simple value = 20 */
    False,
    /** simple value = 21 */
    True,
    /** simple value = 22 */
    Null,
    /** simple value = 23 */
    Undefined,
    /** one byte, that is in range 0..19, 24..255 */
    SimpleValue,
    Float16,
    Float32,
    Float64,
    /** end of indefinite length item */
    Break,
}
