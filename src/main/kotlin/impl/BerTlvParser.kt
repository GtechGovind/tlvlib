package impl

import api.Tlv
import api.TlvParser
import utils.TlvException

/**
 * A robust, production-grade parser for decoding BER-TLV byte streams into [Tlv] objects.
 *
 * This implementation is internal to the library and exposed via the [TlvParser.create] factory method.
 * It is designed to be efficient by minimizing object allocations and unnecessary data copying. It operates
 * by maintaining an index (offset) into the source byte array rather than slicing the array at each step.
 *
 * The parsing logic correctly handles:
 * - Single and multi-byte tags.
 * - Short-form and long-form length encoding.
 * - Recursive parsing of constructed TLV objects.
 * - Comprehensive error checking for malformed or incomplete data streams.
 */
internal class BerTlvParser : TlvParser {

    /**
     * Parses a byte array expected to contain a single root TLV object.
     * Ignores any trailing data after the first complete object.
     */
    override fun parse(data: ByteArray): Tlv {
        if (data.isEmpty()) throw TlvException("Cannot parse an empty byte array.")
        val (tlv, _) = parseOneTlv(data, 0)
        return tlv
    }

    /**
     * Parses a byte array that may contain a list of multiple sibling TLV objects.
     */
    override fun parseList(data: ByteArray): List<Tlv> {
        if (data.isEmpty()) return emptyList()
        val tlvList = mutableListOf<Tlv>()
        var offset = 0
        while (offset < data.size) {
            val (tlv, bytesRead) = parseOneTlv(data, offset)
            tlvList.add(tlv)
            offset += bytesRead
        }
        return tlvList
    }

    /**
     * Core internal function to parse a single TLV object starting from a given offset.
     *
     * @param data The source byte array.
     * @param offset The starting position for parsing.
     * @return A [Pair] containing the parsed [Tlv] object and the total number of bytes consumed.
     * @throws TlvException if parsing fails.
     */
    private fun parseOneTlv(data: ByteArray, offset: Int): Pair<Tlv, Int> {
        var currentOffset = offset

        // 1. Parse Tag
        val (tag, tagBytesRead) = parseTag(data, currentOffset)
        currentOffset += tagBytesRead

        // 2. Parse Length
        val (length, lengthBytesRead) = parseLength(data, currentOffset)
        currentOffset += lengthBytesRead

        // 3. Extract Value
        val valueEndOffset = currentOffset + length
        if (valueEndOffset > data.size) {
            throw TlvException("Incomplete TLV data: value length $length exceeds available data ${data.size - currentOffset} at offset $currentOffset")
        }
        val value = data.copyOfRange(currentOffset, valueEndOffset)

        // 4. Recursively parse children if constructed
        val isConstructed = (tag[0].toInt() and 0x20) != 0
        val children = if (isConstructed) {
            parseList(value)
        } else {
            emptyList()
        }

        val tlv = BerTlv(tag = tag, value = value, children = children)
        val totalBytesConsumed = tagBytesRead + lengthBytesRead + length

        return Pair(tlv, totalBytesConsumed)
    }

    /**
     * Parses the Tag field from the data array. Handles multi-byte tags.
     *
     * BER-TLV Tag format:
     * - If the lower 5 bits of the first byte are all 1s (0x1F), the tag is multi-byte.
     * - Subsequent bytes are part of the tag until a byte is found where the most
     *   significant bit (b8) is 0.
     */
    private fun parseTag(data: ByteArray, offset: Int): Pair<ByteArray, Int> {
        if (offset >= data.size) throw TlvException("Cannot parse tag: insufficient data at offset $offset")
        val firstByte = data[offset]
        var tagEndOffset = offset + 1

        // Check for multi-byte tag
        if ((firstByte.toInt() and 0x1F) == 0x1F) {
            do {
                if (tagEndOffset >= data.size) throw TlvException("Incomplete multi-byte tag at offset $offset")
                val nextByte = data[tagEndOffset]
                tagEndOffset++
                // The last byte of a multibyte tag has its MSB set to 0
            } while ((nextByte.toInt() and 0x80) != 0)
        }

        val tagBytes = data.copyOfRange(offset, tagEndOffset)
        return Pair(tagBytes, tagEndOffset - offset)
    }

    /**
     * Parses the Length field from the data array. Handles short and long forms.
     *
     * BER-TLV Length format:
     * - Short form: If MSB is 0, the byte itself is the length (0-127).
     * - Long form: If MSB is 1, the lower 7 bits indicate the number of subsequent bytes
     *   that encode the actual length. A value of 0x80 is indefinite and not supported here.
     */
    private fun parseLength(data: ByteArray, offset: Int): Pair<Int, Int> {
        if (offset >= data.size) {
            throw TlvException("Cannot parse length: insufficient data at offset $offset")
        }
        val firstByte = data[offset].toInt() and 0xFF

        // Short form: length is encoded in the first byte
        if ((firstByte and 0x80) == 0) {
            return Pair(firstByte, 1)
        }

        // Long form
        val numLengthBytes = firstByte and 0x7F
        if (numLengthBytes == 0) {
            // Indefinite length form is not supported by this parser.
            throw TlvException("Indefinite length form (0x80) is not supported.")
        }
        if (numLengthBytes > 4) {
            // Protect against excessively large length fields causing memory issues.
            // 4 bytes can represent a length up to 2GB, which is a practical limit.
            throw TlvException("Length fields longer than 4 bytes are not supported (got $numLengthBytes)")
        }

        val lengthEndOffset = offset + 1 + numLengthBytes
        if (lengthEndOffset > data.size) {
            throw TlvException("Incomplete length field: expected $numLengthBytes bytes, but only ${data.size - (offset + 1)} available.")
        }

        var length = 0
        for (i in 1..numLengthBytes) {
            length = (length shl 8) + (data[offset + i].toInt() and 0xFF)
        }

        return Pair(length, 1 + numLengthBytes)
    }
}