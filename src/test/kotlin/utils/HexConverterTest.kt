package utils

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("HexConverter Utility")
internal class HexConverterTest {

    @Nested
    @DisplayName("byteArrayToHexString()")
    inner class ByteArrayToHexString {
        @Test
        fun `should convert a byte array to a compact hex string`() {
            val bytes = byteArrayOf(0x9F.toByte(), 0x02, 0xAC.toByte(), 0x10)
            assertThat(HexConverter.byteArrayToHexString(bytes)).isEqualTo("9F02AC10")
        }

        @Test
        fun `should convert a byte array with a separator`() {
            val bytes = byteArrayOf(0x4D.toByte(), 0x61, 0x73, 0x74, 0x65, 0x72)
            assertThat(HexConverter.byteArrayToHexString(bytes, " ")).isEqualTo("4D 61 73 74 65 72")
        }

        @Test
        fun `should return an empty string for an empty byte array`() {
            assertThat(HexConverter.byteArrayToHexString(byteArrayOf())).isEmpty()
        }
    }

    @Nested
    @DisplayName("hexToByteArray()")
    inner class HexToByteArray {
        @Test
        fun `should convert a valid uppercase hex string`() {
            val hex = "9F02AC10"
            val expected = byteArrayOf(0x9F.toByte(), 0x02, 0xAC.toByte(), 0x10)
            assertThat(HexConverter.hexToByteArray(hex)).isEqualTo(expected)
        }

        @Test
        fun `should convert a valid lowercase hex string`() {
            val hex = "9f02ac10"
            val expected = byteArrayOf(0x9F.toByte(), 0x02, 0xAC.toByte(), 0x10)
            assertThat(HexConverter.hexToByteArray(hex)).isEqualTo(expected)
        }

        @Test
        fun `should handle hex strings with spaces`() {
            val hex = "4D 61 73 74 65 72"
            val expected = byteArrayOf(0x4D, 0x61, 0x73, 0x74, 0x65, 0x72)
            assertThat(HexConverter.hexToByteArray(hex)).isEqualTo(expected)
        }

        @Test
        fun `should throw TlvException for strings with odd length`() {
            assertThatThrownBy { HexConverter.hexToByteArray("9F02A") }
                .isInstanceOf(TlvException::class.java)
                .hasMessageContaining("must have an even number of characters")
        }

        @Test
        fun `should throw TlvException for strings with invalid hex characters`() {
            assertThatThrownBy { HexConverter.hexToByteArray("9F02G5") }
                .isInstanceOf(TlvException::class.java)
                .hasMessageContaining("Invalid hex sequence 'G5'")
        }
    }
}