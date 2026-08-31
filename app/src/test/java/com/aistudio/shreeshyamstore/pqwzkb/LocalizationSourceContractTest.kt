package com.aistudio.shreeshyamstore.pqwzkb

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LocalizationSourceContractTest {
    private val sourcePaths = listOf(
        "ui/screens/AuthScreens.kt",
        "ui/screens/BillingAndPaymentScreen.kt",
        "ui/screens/FirstLaunchSetupScreen.kt",
        "ui/screens/HomeScreen.kt",
        "ui/screens/ProductsAndStockScreen.kt",
        "ui/screens/ReportsScreen.kt",
        "ui/screens/SettingsScreen.kt",
        "ui/screens/UdhaarScreen.kt",
        "ui/components/BarcodeScannerDialog.kt"
    )

    @Test
    fun designatedScreensDoNotAddRawMerchantFacingLiterals() {
        val sources = designatedSources()
        assertTrue("Issue #54 source scan must find designated Compose screens", sources.isNotEmpty())

        val violations = sources.flatMap { (path, source) ->
            buildList {
                rawStringRegex("contentDescription").findAll(source).forEach { match ->
                    add("$path:${lineNumber(source, match.range.first)} raw contentDescription = ${match.value}")
                }
                rawVisibleStringMatches(source).forEach { match ->
                    val literal = match.groupValues[1]
                    if (!isAllowedNonMerchantLiteral(literal)) {
                        add("$path:${lineNumber(source, match.range.first)} raw visible literal = ${match.value}")
                    }
                }
                if (inlineLanguageBranchRegex.containsMatchIn(source)) {
                    add("$path contains an inline app-language branch returning raw string literals")
                }
            }
        }

        assertFalse(
            "Move merchant-facing literals into LocaleHelper AppStrings. Violations: ${violations.joinToString()}",
            violations.isNotEmpty()
        )
    }

    private fun designatedSources(): List<Pair<String, String>> {
        val roots = listOf(
            File("src/main/java/com/aistudio/shreeshyamstore/pqwzkb"),
            File("app/src/main/java/com/aistudio/shreeshyamstore/pqwzkb"),
            File("../app/src/main/java/com/aistudio/shreeshyamstore/pqwzkb")
        )
        val root = roots.firstOrNull { it.isDirectory } ?: return emptyList()
        return sourcePaths.mapNotNull { name ->
            val file = File(root, name)
            if (file.isFile) name to file.readText() else null
        }
    }

    private fun rawVisibleStringMatches(source: String): List<MatchResult> = listOf(
        Regex("\\b(?:text|placeholder)\\s*=\\s*\"([^\"]*)\"") ,
        Regex("\\bText\\s*\\(\\s*\"([^\"]*)\"") ,
        Regex("\\bToast\\.makeText\\s*\\([^,]+,\\s*\"([^\"]*)\"")
    ).flatMap { regex -> regex.findAll(source).toList() }

    private fun rawStringRegex(name: String): Regex =
        Regex("\\b$name\\s*=\\s*\"([^\"]*)\"")

    private fun isAllowedNonMerchantLiteral(literal: String): Boolean =
        literal in setOf("₹", "🏬") ||
            literal.contains('$') ||
            literal.matches(Regex("[+\\-\\d\\s.,%:/()]+"))

    private fun lineNumber(source: String, offset: Int): Int =
        source.substring(0, offset).count { it == '\n' } + 1

    private val inlineLanguageBranchRegex = Regex(
        "(?s)if\\s*\\([^)]*appLanguage[^)]*\\)\\s*(?:\\{\\s*)?\"[^\"\\n]+\"\\s*else\\s*\"[^\"\\n]+\"|AppLanguage\\.(?:HINDI|ENGLISH)\\)\\s*[-=]>\\s*\"[^\"]+\""
    )
}
