package api

import impl.BerTlvEncoder

/**
 * An interface for an encoder that serializes [Tlv] objects into BER-TLV byte streams.
 *
 * This interface defines the contract for converting a high-level, object-oriented [Tlv] structure
 * back into its raw binary representation according to the Tag-Length-Value encoding rules,
 * specifically the Basic Encoding Rules (BER-TLV) as defined in ISO/IEC 8825-1.
 *
 * The encoder is responsible for:
 * 1.  Writing the tag bytes.
 * 2.  Calculating and writing the length bytes in the correct format (short or long form).
 * 3.  Writing the value bytes.
 *
 * It provides methods to encode both a single TLV object and a list of sibling TLV objects.
 *
 * ### BER-TLV Length Encoding Specification
 * The length (L) of the value field is encoded as follows:
 * - **Short Form:** If the length is between 0 and 127 (inclusive), a single byte is used,
 *   representing the length directly (e.g., a length of 10 is encoded as `0x0A`).
 * - **Long Form:** If the length is 128 or greater, the first byte indicates the number of
 *   subsequent bytes that represent the length. The first byte has its most significant bit (b8)
 *   set to 1, and the remaining 7 bits (b7-b1) encode the number of length bytes to follow.
 *   For example, a length of 250 (0xFA) is encoded as two bytes: `0x81 FA`. A length of 300
 *   (0x012C) would be encoded as three bytes: `0x82 01 2C`.
 *
 * This interface abstracts the complexities of these encoding rules.
 */
interface TlvEncoder {
    /**
     * Encodes a single [Tlv] object into its byte array representation.
     *
     * This method serializes the given `Tlv` object into a `[Tag][Length][Value]` byte sequence.
     * It handles the BER encoding of the length field automatically, choosing between the short
     * and long forms based on the size of the `tlv.value` byte array.
     *
     * Note: If the provided `tlv` object is constructed (`isConstructed` is true), this method
     * assumes that the `tlv.value` property already contains the fully encoded and concatenated
     * byte stream of its children. The encoder does not recursively encode the `children` list;
     * it simply treats the `value` field as a pre-serialized payload.
     *
     * @param tlv The [Tlv] object to encode. This object must be a valid representation,
     *            containing the tag, length, and value to be serialized.
     * @return The resulting BER-TLV byte array, representing the single `Tlv` object.
     * @throws utils.TlvException if the encoding fails. This can happen, for example, if the
     *                            length of the `value` field is too large to be represented
     *                            by the BER-TLV length-of-length encoding rules.
     */
    fun encode(tlv: Tlv): ByteArray

    /**
     * Encodes a list of sibling [Tlv] objects into a single concatenated byte array.
     *
     * This method iterates through the provided list of `Tlv` objects, encodes each one
     * individually using the `encode(Tlv)` method, and concatenates the results into a
     * single, continuous byte stream. The order of the objects in the list is preserved
     * in the output byte array.
     *
     * This is particularly useful for creating the `value` payload for a constructed TLV object.
     * The output of this method can be used as the `value` for a parent `Tlv` node.
     *
     * @param tlvList The list of [Tlv] objects to encode. These are typically sibling nodes
     *                that should appear sequentially in the output.
     * @return The resulting concatenated BER-TLV byte array, containing all the encoded
     *         objects from the list in order. If the input list is empty, an empty byte
     *         array is returned.
     * @throws utils.TlvException if the encoding fails for any of the `Tlv` objects within the list.
     */
    fun encode(tlvList: List<Tlv>): ByteArray

    /**
     * Provides a factory for creating instances of [TlvEncoder].
     */
    companion object {
        /**
         * Creates and returns a default instance of the BER-TLV encoder.
         *
         * This factory method provides a convenient way to get a standard, concrete
         * implementation of the [TlvEncoder] interface without needing to know the
         * specific implementation class.
         *
         * @return A new instance of a class that implements [TlvEncoder] for BER-TLV encoding.
         */
        fun create(): TlvEncoder = BerTlvEncoder()
    }
}