package utils


/**
 * An internal utility object for converting between byte arrays and hexadecimal strings.
 */
internal object HexConverter {

    /**
     * Converts a ByteArray to an uppercase hexadecimal string.
     *
     * @param bytes The byte array to convert.
     * @param separator An optional separator to insert between byte representations.
     * @return The formatted hexadecimal string.
     */
    fun byteArrayToHexString(bytes: ByteArray, separator: String = ""): String {
        if (bytes.isEmpty()) return ""
        return bytes.joinToString(separator) { "%02X".format(it) }
    }

    /**
     * Converts a hexadecimal string into a ByteArray.
     * Handles strings with or without separators (e.g., spaces, colons, dashes).
     *
     * @param hexString The hexadecimal string to convert.
     * @return The resulting ByteArray.
     * @throws TlvException if the string contains non-hex characters or has an odd length after cleaning.
     */
    fun hexToByteArray(hexString: String): ByteArray {
        val cleanHex = hexString.replace(Regex("[\\s-]"), "")
        if (cleanHex.length % 2 != 0) {
            throw TlvException("Hex string must have an even number of characters: '$hexString'")
        }
        return cleanHex.chunked(2)
            .map {
                try {
                    it.toInt(16).toByte()
                } catch (e: NumberFormatException) {
                    throw TlvException("Invalid hex sequence '$it' in string: '$hexString'", e)
                }
            }
            .toByteArray()
    }
}