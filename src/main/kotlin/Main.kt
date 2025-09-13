import api.Tlv
import api.TlvEncoder
import api.TlvParser
import utils.HexConverter
import utils.TlvException

fun main() {
    // Example data from the FEIG PPSE documentation
    val ppseHex = "6F2F" +
            "840E325041592E5359532E4444463031" +
            "A51D" +
            "BF0C1A" +
            "6118" +
            "4F07A0000000041010" +
            "500A4D617374657243617264" +
            "870101"

    val ppseData = HexConverter.hexToByteArray(ppseHex)

    // 1. Create parser and encoder instances
    val parser = TlvParser.create()
    val encoder = TlvEncoder.create()

    println("--- 1. Parsing BER-TLV Data ---")
    try {
        val rootTlv = parser.parse(ppseData)

        println("\n--- 2. Pretty-Printing Parsed Tree ---")
        printTlvTree(rootTlv)

        println("\n--- 3. Finding Tags ---")

        // Deep find for tag '50' (Application Label)
        val appLabel = rootTlv.deepFind("50")
        if (appLabel != null) {
            println("Found Application Label (Tag 50):")
            println("  - Value as Hex: ${appLabel.valueAsHexString}")
            println("  - Value as ASCII: ${appLabel.valueAsAsciiString}")
            check(appLabel.valueAsAsciiString == "MasterCard")
        } else {
            println("Tag '50' not found.")
        }

        // Shallow find for tag '84' (DF Name) directly under the root
        val dfName = rootTlv.find("84")
        if (dfName != null) {
            println("Found DF Name (Tag 84) at root level:")
            println("  - Value as Hex: ${dfName.valueAsHexString}")
        } else {
            println("Tag '84' not found at root level.")
        }

        println("\n--- 4. Encoding TLV Object Back to Bytes ---")
        val encodedBytes = encoder.encode(rootTlv)

        println("Original Hex:  $ppseHex")
        println("Encoded Hex:   ${HexConverter.byteArrayToHexString(encodedBytes)}")

        check(ppseData.contentEquals(encodedBytes))
        println("\nSUCCESS: Original and re-encoded byte arrays match!")

    } catch (e: TlvException) {
        System.err.println("ERROR: Parsing failed - ${e.message}")
        e.printStackTrace()
    }
}

/**
 * A helper function to recursively print the TLV tree structure for visualization.
 */
fun printTlvTree(tlv: Tlv, indent: String = "") {
    val tag = tlv.tagAsHexString.padEnd(6)
    val valueInfo = if (tlv.isConstructed) {
        "(constructed)"
    } else {
        "[${tlv.valueAsAsciiString}]"
    }
    println("$indent- Tag: $tag  Length: ${tlv.length.toString().padEnd(4)}  Value: $valueInfo")

    tlv.children.forEach { printTlvTree(it, "$indent  ") }
}