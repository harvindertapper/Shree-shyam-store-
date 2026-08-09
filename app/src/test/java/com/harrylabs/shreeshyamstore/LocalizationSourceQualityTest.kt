package com.harrylabs.shreeshyamstore

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class LocalizationSourceQualityTest {

    @Test
    fun englishAndHindiStringResourcesHaveMatchingKeys() {
        val englishKeys = stringNames(File("src/main/res/values/strings.xml"))
        val hindiKeys = stringNames(File("src/main/res/values-hi/strings.xml"))

        assertEquals(englishKeys, hindiKeys)
    }

    @Test
    fun localizedScreensDoNotContainMojibakeOrMixedTranslationCopy() {
        val targetFiles = listOf(
            "src/main/java/com/harrylabs/shreeshyamstore/ui/screens/BillingAndPaymentScreen.kt",
            "src/main/java/com/harrylabs/shreeshyamstore/ui/screens/ProductsAndStockScreen.kt",
            "src/main/java/com/harrylabs/shreeshyamstore/ui/screens/ReportsScreen.kt",
            "src/main/java/com/harrylabs/shreeshyamstore/ui/screens/UdhaarScreen.kt",
            "src/main/java/com/harrylabs/shreeshyamstore/viewmodel/ShopViewModel.kt"
        )
        val disallowedPatterns = listOf(
            "à¤",
            "ðŸ",
            Regex("\\([^)]*(New|Login|Register|Payment|Cash|Udhaar|Product|Stock|Settings|Reports|Today|All|Close|Save|Customer|Opening)[^)]*\\)")
        )

        val findings = targetFiles.flatMap { path ->
            val file = File(path)
            file.readLines().mapIndexedNotNull { index, line ->
                val hasFinding = line.contains("\"") && disallowedPatterns.any { pattern ->
                    when (pattern) {
                        is String -> line.contains(pattern)
                        is Regex -> pattern.containsMatchIn(line)
                        else -> false
                    }
                }
                if (hasFinding) "${file.name}:${index + 1}: ${line.trim()}" else null
            }
        }

        assertFalse(findings.joinToString(separator = "\n"), findings.isNotEmpty())
    }

    private fun stringNames(file: File): Set<String> {
        val document = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(file)
        val nodes = document.getElementsByTagName("string")
        return (0 until nodes.length)
            .map { nodes.item(it).attributes.getNamedItem("name").nodeValue }
            .toSet()
    }
}
