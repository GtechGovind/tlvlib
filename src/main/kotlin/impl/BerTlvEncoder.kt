package impl

import api.Tlv
import api.TlvEncoder
import utils.TlvException
import java.io.ByteArrayOutputStream

/**
 * An encoder for serializing [api.Tlv] objects into BER-TLV byte streams.
 */
internal class BerTlvEncoder : TlvEncoder {

    override fun encode(tlv: Tlv): ByteArray {
        val lengthBytes = encodeLength(tlv.length)
        return tlv.tag + lengthBytes + tlv.value
    }

    override fun encode(tlvList: List<Tlv>): ByteArray {
        val stream = ByteArrayOutputStream()
        tlvList.forEach { tlv ->
            stream.write(encode(tlv))
        }
        return stream.toByteArray()
    }

    private fun encodeLength(length: Int): ByteArray {
        return when {
            length < 0 -> throw TlvException("Length cannot be negative: $length")

            // Short form
            length <= 127 -> byteArrayOf(length.toByte())

            // Long form
            length <= 0xFF -> byteArrayOf(0x81.toByte(), length.toByte())
            length <= 0xFFFF -> byteArrayOf(
                0x82.toByte(),
                (length ushr 8).toByte(),
                length.toByte()
            )
            length <= 0xFFFFFF -> byteArrayOf(
                0x83.toByte(),
                (length ushr 16).toByte(),
                (length ushr 8).toByte(),
                length.toByte()
            )
            // Note: 4-byte length is the practical limit for many systems
            else -> byteArrayOf(
                0x84.toByte(),
                (length ushr 24).toByte(),
                (length ushr 16).toByte(),
                (length ushr 8).toByte(),
                length.toByte()
            )
        }
    }
}