package api

import utils.HexConverter

/**
 * Represents a single parsed TLV (Tag-Length-Value) data object.
 *
 * This is an immutable, read-only interface for a node in a TLV tree structure.
 * It provides convenient accessors for the tag and value in various formats.
 *
 * ### TLV (Tag-Length-Value) Specification
 *
 * TLV is an encoding scheme used to represent data with a flexible structure. Each data element
 * is encoded as a triplet of fields:
 *
 * 1.  **Tag:** A unique identifier for the data element. According to specifications like those
 *     in EMV, the tag is a one or more byte value that defines the meaning of the data.
 *
 * 2.  **Length:** Specifies the size of the `Value` field in bytes. The length itself can be
 *     encoded in one or more bytes (short form vs. long form).
 *
 * 3.  **Value:** The actual data payload. The value can be either:
 *     - **Primitive:** Contains a simple data element (e.g., a number, string, or raw bytes).
 *     - **Constructed:** Contains a sequence of other, nested TLV data objects. This allows for
 *       the creation of complex, hierarchical data structures.
 *
 * A TLV object is considered "constructed" if the 6th bit (b6) of the first byte of its tag is set to 1.
 * Otherwise, it is "primitive". This interface provides the `isConstructed` property to easily
 * check this.
 *
 * This interface is designed to be agnostic of the underlying parsing implementation, focusing
 * only on providing a standardized way to access the parsed data.
 */
interface Tlv {
    /**
     * The raw bytes of the tag.
     *
     * In standards like EMV, tags can be one, two, or more bytes long. This property provides the
     * complete, unmodified byte sequence that represents the tag.
     *
     * Example: A tag `9F02` would be represented as `byteArrayOf(0x9F.toByte(), 0x02.toByte())`.
     */
    val tag: ByteArray

    /**
     * The integer length of the value field.
     *
     * This represents the number of bytes in the `value` property. It is the decoded length
     * from the "L" part of the TLV structure.
     */
    val length: Int

    /**
     * The raw bytes of the value field.
     *
     * For primitive tags, this contains the actual data payload. For constructed tags, this
     * contains the concatenated, encoded byte stream of all child TLV objects.
     */
    val value: ByteArray

    /**
     * A list of child TLV nodes.
     *
     * This list is populated only if the tag is constructed (`isConstructed` is true). It contains
     * the parsed `Tlv` objects that were nested within the current object's value. For primitive
     * tags, this list is always empty.
     */
    val children: List<Tlv>

    /**
     * A boolean flag indicating if this is a constructed tag.
     *
     * A constructed tag's value is composed of one or more nested TLV objects. A primitive tag's
     * value is a simple data element. This flag is determined by checking the 6th bit of the
     * first byte of the `tag`.
     *
     * @return `true` if the tag is constructed, `false` if it is primitive.
     */
    val isConstructed: Boolean

    /**
     * The tag represented as a compact uppercase hexadecimal string.
     *
     * This provides a human-readable representation of the `tag` bytes.
     *
     * Example: A `tag` of `byteArrayOf(0x9F.toByte(), 0x02.toByte())` becomes `"9F02"`.
     * A `tag` of `byteArrayOf(0xDF.toByte(), 0x81.toByte(), 0x2A.toByte())` becomes `"DF812A"`.
     */
    val tagAsHexString: String

    /**
     * The value represented as an uppercase hexadecimal string, with spaces between bytes.
     *
     * This formatting enhances readability for binary data.
     *
     * Example: A `value` of `byteArrayOf(0x4D, 0x61, 0x73, 0x74, 0x65, 0x72)` becomes `"4D 61 73 74 65 72"`.
     */
    val valueAsHexString: String

    /**
     * The value interpreted as an ASCII string.
     *
     * Each byte in the `value` array is interpreted as an ASCII character. Any byte that does not
     * correspond to a printable ASCII character (codes 32-126) is replaced with a dot ('.').
     *
     * This is useful for quickly inspecting values that may contain human-readable text,
     * such as cardholder names or application labels.
     */
    val valueAsAsciiString: String

    /**
     * Performs a shallow search for a TLV object with the given tag among the direct children of this node.
     *
     * "Shallow" means this search only inspects the `children` list of the current `Tlv` object.
     * It does not recursively search into the children of children.
     *
     * @param tag The tag to search for, represented as a hex string (e.g., "9F02").
     *            This string is case-insensitive and is converted to a byte array for the search.
     * @return The first matching child `Tlv` object, or `null` if no direct child with that tag is found.
     */
    fun find(tag: String): Tlv? = find(HexConverter.hexToByteArray(tag))

    /**
     * Performs a shallow search for a TLV object with the given tag among the direct children of this node.
     *
     * This is the primitive search function. It directly compares the byte array of the requested tag
     * against the tags of each `Tlv` object in the `children` list.
     *
     * @param tag The tag to search for, represented as a `ByteArray`.
     * @return The first matching child `Tlv` object, or `null` if not found.
     */
    fun find(tag: ByteArray): Tlv?

    /**
     * Performs a deep, depth-first search for a TLV object with the given tag anywhere in the tree below this node.
     *
     * This search is recursive. It traverses the entire subtree rooted at the current node in a
     * depth-first manner, meaning it explores as far as possible along each branch before backtracking.
     * It returns the very first match it finds during this traversal.
     *
     * @param tag The tag to search for, represented as a hex string (e.g., "50").
     *            This string is case-insensitive.
     * @return The first matching `Tlv` object found anywhere in the descendant tree, or `null` if not found.
     */
    fun deepFind(tag: String): Tlv? = deepFind(HexConverter.hexToByteArray(tag))

    /**
     * Performs a deep, depth-first search for a TLV object with the given tag anywhere in the tree below this node.
     *
     * This is the primitive deep search function. It recursively checks the current node's children,
     * and if a child is constructed, it continues the search within that child's subtree.
     *
     * @param tag The tag to search for, represented as a `ByteArray`.
     * @return The first matching `Tlv` object found in the descendant tree, or `null` if not found.
     */
    fun deepFind(tag: ByteArray): Tlv?
}