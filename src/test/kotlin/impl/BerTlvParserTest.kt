package impl

import api.TlvParser
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import utils.HexConverter
import utils.TlvException

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

@DisplayName("BerTlvParser Implementation")
internal class BerTlvParserTest {

    private val parser: TlvParser = BerTlvParser()
    // This property remains a ByteArray for other tests that need it.
    private val ppseData = HexConverter.hexToByteArray(VALID_PPSE_HEX)

    @Nested
    @DisplayName("parse() for a single TLV object")
    inner class ParseSingle {
        @Test
        fun `should parse a simple primitive TLV`() {
            val tlv = parser.parse(HexConverter.hexToByteArray("9F0206000000001234"))
            assertThat(tlv.tagAsHexString).isEqualTo("9F02")
            assertThat(tlv.length).isEqualTo(6)
            assertThat(tlv.isConstructed).isFalse()
            assertThat(tlv.children).isEmpty()
        }

        @Test
        fun `should correctly parse a complex, nested TLV structure`() {
            val root = parser.parse(ppseData)
            assertThat(root.tagAsHexString).isEqualTo("6F")
            assertThat(root.isConstructed).isTrue()
            // The root '6F' tag has two direct children: '84' and 'A5'
            assertThat(root.children).hasSize(2)

            // Check direct child
            val dfName = root.find("84")!!
            assertThat(dfName.valueAsAsciiString).isEqualTo("2PAY.SYS.DDF01")

            // Check nested child
            val fciTemplate = root.find("A5")!!
            assertThat(fciTemplate.isConstructed).isTrue()
            val appPriority = fciTemplate.find("87")!!
            assertThat(appPriority.valueAsHexString).isEqualTo("01")

            // Check deeply nested child using deepFind
            val appLabel = root.deepFind("50")!!
            assertThat(appLabel.valueAsAsciiString).isEqualTo("MS")
        }

        @Test
        fun `should handle long-form length encoding (1 byte)`() {
            // Length 0x81 -> 1 byte follows. The length is 0x90 = 144
            val hex = "5F2D8190" + "454E".repeat(72) // 144 bytes of value
            val tlv = parser.parse(HexConverter.hexToByteArray(hex))
            assertThat(tlv.tagAsHexString).isEqualTo("5F2D")
            assertThat(tlv.length).isEqualTo(144)
        }

        @Test
        fun `should parse correctly and ignore trailing data`() {
            val tlv = parser.parse(HexConverter.hexToByteArray("50024D53" + "FFFF")) // Trailing FFFF
            assertThat(tlv.tagAsHexString).isEqualTo("50")
            assertThat(tlv.length).isEqualTo(2)
        }

        @Test
        fun `should throw TlvException for empty data`() {
            assertThatThrownBy { parser.parse(byteArrayOf()) }
                .isInstanceOf(TlvException::class.java)
                .hasMessage("Cannot parse an empty byte array.")
        }

        @Test
        fun `should throw TlvException for incomplete value data`() {
            // Tag 50, Length 10, but only 2 bytes of value provided
            assertThatThrownBy { parser.parse(HexConverter.hexToByteArray("500A4D53")) }
                .isInstanceOf(TlvException::class.java)
                .hasMessageContaining("Incomplete TLV data: value length 10 exceeds available data 2")
        }
    }

    @Nested
    @DisplayName("parseList() for multiple sibling TLV objects")
    inner class ParseList {
        @Test
        fun `should parse a list of primitive TLVs`() {
            val listData = HexConverter.hexToByteArray("50024D53" + "9F020101")
            val tlvList = parser.parseList(listData)
            assertThat(tlvList).hasSize(2)
            assertThat(tlvList[0].tagAsHexString).isEqualTo("50")
            assertThat(tlvList[1].tagAsHexString).isEqualTo("9F02")
        }

        @Test
        fun `should parse a list containing constructed TLVs`() {
            // ##### THE FIX IS HERE #####
            // Concatenate the hex STRINGS first, then convert the final string to a byte array.
            val listHex = "50024D53" + VALID_PPSE_HEX
            val listData = HexConverter.hexToByteArray(listHex)
            // ##### END OF FIX #####

            val tlvList = parser.parseList(listData)
            assertThat(tlvList).hasSize(2)
            assertThat(tlvList[0].isConstructed).isFalse()
            assertThat(tlvList[1].isConstructed).isTrue()
            assertThat(tlvList[1].find("84")).isNotNull
        }

        @Test
        fun `should return an empty list for empty data`() {
            assertThat(parser.parseList(byteArrayOf())).isEmpty()
        }
    }
}