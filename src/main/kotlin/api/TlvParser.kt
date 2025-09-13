package api

import impl.BerTlvParser

/**
 * A public interface for a parser that decodes BER-TLV byte streams into structured [Tlv] objects.
 *
 * This interface defines the contract for a parser that can transform a raw byte array,
 * formatted according to the Basic Encoding Rules for Tag-Length-Value (BER-TLV), into a
 * navigable tree of [Tlv] objects. The parser is responsible for handling the complexities
 * of the BER-TLV format, such as multi-byte tags and the short/long forms of length encoding.
 *
 * The parsing process involves:
 * 1.  **Reading the Tag:** Identifying the one or more bytes that constitute the tag.
 * 2.  **Decoding the Length:** Parsing the length field to determine the size of the value. This
 *     includes correctly interpreting single-byte (short form) and multi-byte (long form) lengths.
 * 3.  **Extracting the Value:** Reading the specified number of bytes for the value field.
 * 4.  **Recursive Parsing:** If a TLV object is identified as "constructed" (based on its tag),
 *     the parser recursively applies the same logic to its value field to build a nested
 *     tree of [Tlv] objects.
 *
 * This interface provides methods for parsing both single root-level TLV objects and lists
 * of sibling TLV objects.
 */
interface TlvParser {
    /**
     * Parses a byte array that is expected to contain a single root TLV object.
     *
     * This method is designed for scenarios where the input byte array represents one complete
     * TLV structure (which may itself be constructed and contain nested TLVs). If the byte
     * array contains more data after the first complete TLV object, that trailing data is ignored.
     * This behavior is useful for extracting a specific TLV object from the beginning of a larger data stream.
     *
     * @param data The raw BER-TLV encoded byte array. It must not be empty.
     * @return The parsed [Tlv] object, which serves as the root of the parsed data tree.
     * @throws utils.TlvException if the data is malformed, incomplete (e.g., length specifies more
     *                            bytes than are available), or if the input `data` array is empty.
     */
    fun parse(data: ByteArray): Tlv

    /**
     * Parses a byte array that may contain a list of multiple sibling TLV objects concatenated together.
     *
     * This method reads the input byte array from start to finish, parsing each consecutive TLV
     * object it finds. It is suitable for data streams that represent a sequence of data elements
     * at the same level, such as the contents of a constructed TLV's value field.
     *
     * The parser will continue until all bytes in the array have been consumed.
     *
     * @param data The raw BER-TLV encoded byte array, potentially containing multiple concatenated TLV objects.
     * @return A list of root-level [Tlv] objects parsed from the data. The list preserves the order
     *         in which the objects appeared in the byte array. Returns an empty list if the input `data`
     *         is empty.
     * @throws utils.TlvException if the data is malformed at any point (e.g., an incomplete TLV object
     *                            at the end of the stream). If an error occurs, the parsing process
     *                            is halted and the exception is thrown.
     */
    fun parseList(data: ByteArray): List<Tlv>

    /**
     * Provides a factory for creating instances of [TlvParser].
     */
    companion object {
        /**
         * Creates and returns a default instance of the BER-TLV parser.
         *
         * This factory method is the standard entry point for obtaining a parser. It abstracts away
         * the concrete implementation class, allowing for flexibility and adherence to the principle of
         * programming to an interface.
         *
         * @return A new instance of a class that implements [TlvParser].
         */
        fun create(): TlvParser = BerTlvParser()
    }
}