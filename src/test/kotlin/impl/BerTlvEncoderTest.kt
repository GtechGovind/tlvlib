package impl

import api.TlvEncoder
import api.TlvParser
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import utils.HexConverter

/**
 * This constant holds the fully corrected BER-TLV data for the PPSE response.
 * The original test data had multiple incorrect length fields, which have been
 * recalculated and fixed here. This valid string is used across multiple tests.
 */
private const val VALID_PPSE_HEX = "6F26" +                        // FCI Template, L=38
        "840E325041592E5359532E4444463031" +    // DF Name, L=14
        "A514" +                            // FCI Prop. Template, L=20
        "870101" +                          // App Priority, L=1
        "BF0C0E" +                          // FCI Issuer Disc. Data, L=14
        "610C" +                        // Application Template, L=12
        "4F06A00000000410" +        // AID, L=6
        "50024D53"                  // App Label, L=2 ('MS')

@DisplayName("BerTlvEncoder Implementation")
internal class BerTlvEncoderTest {

    private val encoder: TlvEncoder = BerTlvEncoder()
    private val parser: TlvParser = BerTlvParser()

    @Nested
    @DisplayName("encode() a single TLV")
    inner class EncodeSingle {
        @Test
        fun `should encode a primitive TLV with short-form length`() {
            val tlv = TlvNode(
                tag = HexConverter.hexToByteArray("50"),
                value = "MasterCard".toByteArray(),
                isConstructed = false
            )
            val expected = "500A" + HexConverter.byteArrayToHexString("MasterCard".toByteArray())
            assertThat(encoder.encode(tlv)).isEqualTo(HexConverter.hexToByteArray(expected))
        }

        @Test
        fun `should encode a primitive TLV with long-form length`() {
            // Length 144 requires 0x81 0x90
            val longValue = ByteArray(144) { 0xAA.toByte() }
            val tlv = TlvNode(
                tag = HexConverter.hexToByteArray("DF8129"),
                value = longValue,
                isConstructed = false
            )
            val encoded = encoder.encode(tlv)
            val expectedStart = HexConverter.hexToByteArray("DF81298190")
            assertThat(encoded).startsWith(*expectedStart)
            assertThat(encoded.size).isEqualTo(3 + 2 + 144) // Tag + Length + Value
        }
    }

    @Nested
    @DisplayName("encode() a list of TLVs")
    inner class EncodeList {
        @Test
        fun `should correctly concatenate a list of encoded TLVs`() {
            val tlv1 = TlvNode(HexConverter.hexToByteArray("50"), "MS".toByteArray(), false)
            val tlv2 = TlvNode(HexConverter.hexToByteArray("9F02"), HexConverter.hexToByteArray("1234"), false)
            val encodedList = encoder.encode(listOf(tlv1, tlv2))

            val expected = HexConverter.hexToByteArray("50024D53" + "9F02021234")
            assertThat(encodedList).isEqualTo(expected)
        }
    }

    @Nested
    @DisplayName("Round-Trip Test")
    inner class RoundTrip {
        @Test
        fun `parsing then encoding should result in the original byte array`() {
            // Use the corrected, valid hex string
            val originalBytes = HexConverter.hexToByteArray(VALID_PPSE_HEX)

            // 1. Parse the valid original data
            val parsedTlv = parser.parse(originalBytes)

            // 2. Re-encode the parsed object
            val reEncodedBytes = encoder.encode(parsedTlv)

            // 3. Assert that the result matches the original
            assertThat(HexConverter.byteArrayToHexString(reEncodedBytes))
                .withFailMessage("Re-encoded hex string did not match the original")
                .isEqualTo(VALID_PPSE_HEX)

            assertThat(reEncodedBytes)
                .withFailMessage("Re-encoded byte array did not match the original")
                .isEqualTo(originalBytes)

            println("Round-trip test successful!")
        }
    }
}