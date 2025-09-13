package impl

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import utils.HexConverter

@DisplayName("TlvNode Data Class")
internal class TlvNodeTest {

    private lateinit var root: TlvNode
    private lateinit var child1: TlvNode
    private lateinit var child2: TlvNode
    private lateinit var grandchild: TlvNode

    @BeforeEach
    fun setUp() {
        // Create a nested tree structure for testing search functions
        // E1 (root)
        // |- 50 (child1)
        // |- E2 (child2)
        //    |- 9F02 (grandchild)
        grandchild = TlvNode(
            tag = HexConverter.hexToByteArray("9F02"),
            value = HexConverter.hexToByteArray("1234"),
            isConstructed = false
        )
        child1 = TlvNode(
            tag = HexConverter.hexToByteArray("50"),
            value = HexConverter.hexToByteArray("4D53"), // "MS"
            isConstructed = false
        )
        child2 = TlvNode(
            tag = HexConverter.hexToByteArray("E2"),
            value = HexConverter.hexToByteArray("9F02021234"), // Dummy value
            isConstructed = true,
            children = listOf(grandchild)
        )
        root = TlvNode(
            tag = HexConverter.hexToByteArray("E1"),
            value = HexConverter.hexToByteArray("50024D53E2069F02021234"), // Dummy value
            isConstructed = true,
            children = listOf(child1, child2)
        )
    }

    @Nested
    @DisplayName("Property Accessors")
    inner class Accessors {
        @Test
        fun `isConstructed should be correct`() {
            assertThat(root.isConstructed).isTrue()
            assertThat(child1.isConstructed).isFalse()
        }

        @Test
        fun `valueAsAsciiString should replace non-printable characters`() {
            val node = TlvNode(
                tag = HexConverter.hexToByteArray("DF01"),
                value = byteArrayOf('A'.code.toByte(), 'B'.code.toByte(), 0x01, 'C'.code.toByte()),
                isConstructed = false
            )
            assertThat(node.valueAsAsciiString).isEqualTo("AB.C")
        }
    }

    @Nested
    @DisplayName("Search Functions")
    inner class Search {
        @Test
        fun `find() should return a direct child`() {
            assertThat(root.find("50")).isEqualTo(child1)
            assertThat(root.find(HexConverter.hexToByteArray("E2"))).isEqualTo(child2)
        }

        @Test
        fun `find() should return null for a nested child`() {
            assertThat(root.find("9F02")).isNull()
        }

        @Test
        fun `deepFind() should return a direct child`() {
            assertThat(root.deepFind("50")).isEqualTo(child1)
        }

        @Test
        fun `deepFind() should return a deeply nested child`() {
            assertThat(root.deepFind("9F02")).isEqualTo(grandchild)
        }

        @Test
        fun `deepFind() should return null if tag does not exist`() {
            assertThat(root.deepFind("FFFF")).isNull()
        }
    }

    @Nested
    @DisplayName("Equality and Hashing")
    inner class Equality {
        @Test
        fun `two nodes with same content should be equal`() {
            val node1 = TlvNode(HexConverter.hexToByteArray("9F02"), HexConverter.hexToByteArray("1234"), false)
            val node2 = TlvNode(HexConverter.hexToByteArray("9F02"), HexConverter.hexToByteArray("1234"), false)
            assertThat(node1).isEqualTo(node2)
            assertThat(node1.hashCode()).isEqualTo(node2.hashCode())
        }

        @Test
        fun `two nodes with different content should not be equal`() {
            val node1 = TlvNode(HexConverter.hexToByteArray("9F02"), HexConverter.hexToByteArray("1234"), false)
            val node2 = TlvNode(HexConverter.hexToByteArray("9F03"), HexConverter.hexToByteArray("1234"), false)
            assertThat(node1).isNotEqualTo(node2)
        }
    }
}