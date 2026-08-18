package com.aistudio.shreeshyamstore.pqwzkb

import com.aistudio.shreeshyamstore.pqwzkb.utils.SecurityUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityUtilsTest {
    @Test
    fun hashPin_isDeterministicSha256Hex() {
        val hash = SecurityUtils.hashPin("1234")

        assertEquals(64, hash.length)
        assertEquals(hash, SecurityUtils.hashPin(" 1234 "))
        assertTrue(hash.all { it in "0123456789abcdef" })
    }

    @Test
    fun verifyPin_acceptsHashedAndLegacyValues() {
        assertTrue(SecurityUtils.verifyPin("1234", SecurityUtils.hashPin("1234")))
        assertTrue(SecurityUtils.verifyPin("1234", "1234"))
        assertTrue(SecurityUtils.verifyPin("1234", ""))
    }

    @Test
    fun verifyPin_rejectsWrongOrMalformedValues() {
        assertFalse(SecurityUtils.verifyPin("0000", SecurityUtils.hashPin("1234")))
        assertFalse(SecurityUtils.verifyPin("1234", "12ab"))
        assertFalse(SecurityUtils.verifyPin("", SecurityUtils.hashPin("1234")))
    }
}
