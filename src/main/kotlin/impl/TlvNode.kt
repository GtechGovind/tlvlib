package impl

import api.Tlv
import utils.HexConverter
import java.nio.charset.StandardCharsets

/**
 * An internal, immutable, concrete implementation of the [Tlv] interface.
 *
 * This data class serves as the default data holder for a single parsed TLV node within the library.
 * It is designed to be a pure data container, with its properties populated by the [BerTlvParser].
 * The visibility is `internal`, meaning it is not exposed as part of the public API, encouraging
 * users of the library to program against the [Tlv] interface.
 *
 * ### Key Implementation Details:
 *
 * - **Immutability:** All properties are read-only (`val`), making instances of `TlvNode`
 *   immutable and thread-safe.
 *
 * - **Lazy Computation:** Derivative properties like `tagAsHexString`, `valueAsHexString`, and
 *   `valueAsAsciiString` are implemented using `by lazy`. This is a performance optimization
 *   that defers the potentially expensive conversion work until the property is accessed for the
 *   first time. The result is then cached for subsequent accesses.
 *
 * - **Content-Based Equality:** The `equals()` and `hashCode()` methods are explicitly overridden.
 *   This is crucial because the default `data class` implementation for `ByteArray` properties
 *   performs a referential equality check (`===`), not a structural one. The overrides ensure
 *   that two `TlvNode` instances are considered equal if their tags, values, and other properties
 *   are content-wise identical. This is essential for predictable behavior in collections (e.g., `Set`, `Map`)
 *   and for reliable unit testing.
 *
 * @property tag The raw bytes of the tag.
 * @property value The raw bytes of the value field.
 * @property isConstructed A pre-calculated boolean flag indicating if the tag is a constructed type. This
 *                         is determined by the parser based on the tag's first byte.
 * @property children A list of child [Tlv] nodes. For primitive (non-constructed) TLV objects,
 *                    this list is empty.
 */
internal data class TlvNode(
    override val tag: ByteArray,
    override val value: ByteArray,
    override val isConstructed: Boolean,
    override val children: List<Tlv> = emptyList()
) : Tlv {

    /**
     * The integer length of the value field, derived directly from the size of the `value` byte array.
     */
    override val length: Int get() = value.size

    /**
     * A lazily-initialized, uppercase hexadecimal string representation of the `tag`.
     * Example: `9F02`
     */
    override val tagAsHexString: String by lazy { HexConverter.byteArrayToHexString(tag) }

    /**
     * A lazily-initialized, space-separated, uppercase hexadecimal string representation of the `value`.
     * Example: `01 02 03`
     */
    override val valueAsHexString: String by lazy { HexConverter.byteArrayToHexString(value, " ") }

    /**
     * A lazily-initialized ASCII string representation of the `value`.
     * Each byte is interpreted as a US-ASCII character. Any non-printable characters are
     * replaced with a period (`.`) for safe display.
     */
    override val valueAsAsciiString: String by lazy {
        // The regex replaces any character that is not in the "Printable" Unicode character property class.
        value.toString(StandardCharsets.US_ASCII).replace(Regex("[^\\p{Print}]"), ".")
    }

    /**
     * Performs a shallow search for a TLV object with the given tag among the direct children of this node.
     *
     * @param tag The tag to search for, as a `ByteArray`.
     * @return The first matching child [Tlv] object, or `null` if not found.
     */
    override fun find(tag: ByteArray): Tlv? {
        // contentEquals is used for correct byte-array-to-byte-array comparison.
        return children.find { it.tag.contentEquals(tag) }
    }

    /**
     * Performs a deep, depth-first search for a TLV object with the given tag anywhere in the tree below this node.
     * This is implemented recursively.
     *
     * @param tag The tag to search for, as a `ByteArray`.
     * @return The first matching [Tlv] object in the tree, or `null` if not found.
     */
    override fun deepFind(tag: ByteArray): Tlv? {
        for (child in children) {
            if (child.tag.contentEquals(tag)) {
                return child
            }
            // Recurse into the child's subtree
            val foundInChild = child.deepFind(tag)
            if (foundInChild != null) {
                return foundInChild
            }
        }
        return null
    }

    /**
     * Provides a concise string representation of the node for logging and debugging.
     * It includes the tag as a hex string, the length, and the constructed status, but omits the
     * potentially long value and children for brevity.
     */
    override fun toString(): String {
        return "TlvNode(tag=$tagAsHexString, length=$length, isConstructed=$isConstructed)"
    }

    /**
     * Overrides the default `equals` to perform a deep, content-based comparison.
     * This is necessary because `ByteArray` properties are not compared by content in a standard data class.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as TlvNode
        if (!tag.contentEquals(other.tag)) return false
        if (!value.contentEquals(other.value)) return false
        if (isConstructed != other.isConstructed) return false
        // The default `equals` for a List of data classes works correctly here because TlvNode has a correct `equals`.
        if (children != other.children) return false
        return true
    }

    /**
     * Overrides the default `hashCode` to be consistent with the custom `equals` method.
     * It computes a hash code based on the content of the `tag` and `value` byte arrays.
     */
    override fun hashCode(): Int {
        var result = tag.contentHashCode()
        result = 31 * result + value.contentHashCode()
        result = 31 * result + isConstructed.hashCode()
        result = 31 * result + children.hashCode()
        return result
    }
}