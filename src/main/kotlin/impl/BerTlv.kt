package impl

import api.Tlv
import utils.HexConverter

/**
 * A concrete, immutable data class implementation of the [Tlv] interface.
 *
 * This class holds the parsed data for a single BER-TLV node. Properties that require
 * computation (like hexadecimal or ASCII string representations) are implemented using
 * lazy delegates. This ensures the conversion is only performed once and only when the
 * property is first accessed, which is an efficient approach for production systems.
 *
 * @property tag The raw bytes of the tag.
 * @property value The raw bytes of the value field.
 * @property children A list of child TLV nodes; empty for primitive tags.
 */
internal data class BerTlv(
    override val tag: ByteArray,
    override val value: ByteArray,
    override val children: List<Tlv> = emptyList()
) : Tlv {

    /**
     * The decoded integer length of the value field.
     */
    override val length: Int = value.size

    /**
     * A boolean flag indicating if this is a constructed tag.
     * In BER-TLV, this is determined by bit 6 of the first tag byte.
     */
    override val isConstructed: Boolean = (tag[0].toInt() and 0x20) != 0

    /**
     * Lazily-computed hexadecimal string representation of the tag.
     */
    override val tagAsHexString: String by lazy {
        HexConverter.byteArrayToHexString(tag)
    }

    /**
     * Lazily-computed, space-separated hexadecimal string representation of the value.
     */
    override val valueAsHexString: String by lazy {
        HexConverter.byteArrayToHexString(value, separator = " ")
    }

    /**
     * Lazily-computed ASCII string representation of the value.
     * Non-printable characters are replaced with a dot ('.').
     */
    override val valueAsAsciiString: String by lazy {
        value.map { byte ->
            val char = byte.toInt().toChar()
            if (char.isPrintable()) char else '.'
        }.joinToString("")
    }

    /**
     * Performs a shallow search for a child TLV with the given tag.
     */
    override fun find(tag: ByteArray): Tlv? {
        return children.find { it.tag.contentEquals(tag) }
    }

    /**
     * Performs a deep, depth-first search for a TLV with the given tag.
     * This implementation uses a classic recursive approach. For extremely deep TLV
     * structures (thousands of levels), an iterative approach using a stack could be
     * considered to prevent potential StackOverflowError, but recursion is clear and
     * sufficient for virtually all real-world data.
     */
    override fun deepFind(tag: ByteArray): Tlv? {
        for (child in children) {
            if (child.tag.contentEquals(tag)) {
                return child
            }
            val found = child.deepFind(tag)
            if (found != null) {
                return found
            }
        }
        return null
    }

    // Auto-generated equals, hashCode, and toString are sufficient for this data class.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BerTlv

        if (length != other.length) return false
        if (isConstructed != other.isConstructed) return false
        if (!tag.contentEquals(other.tag)) return false
        if (!value.contentEquals(other.value)) return false
        if (children != other.children) return false
        if (tagAsHexString != other.tagAsHexString) return false
        if (valueAsHexString != other.valueAsHexString) return false
        if (valueAsAsciiString != other.valueAsAsciiString) return false

        return true
    }

    override fun hashCode(): Int {
        var result = length
        result = 31 * result + isConstructed.hashCode()
        result = 31 * result + tag.contentHashCode()
        result = 31 * result + value.contentHashCode()
        result = 31 * result + children.hashCode()
        result = 31 * result + tagAsHexString.hashCode()
        result = 31 * result + valueAsHexString.hashCode()
        result = 31 * result + valueAsAsciiString.hashCode()
        return result
    }
}

/**
 * A private helper extension function to determine if a character is printable ASCII.
 */
private fun Char.isPrintable(): Boolean {
    val code = this.code
    return code in 32..126
}