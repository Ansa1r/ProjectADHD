package com.ansa1r.projectadhd

import com.ansa1r.projectadhd.domain.profile.Nickname
import org.junit.Assert.*
import org.junit.Test

class NicknameTest {
    @Test fun trimsAndPreservesUnicode() { assertEquals("Максим 🌿", Nickname.normalize("  Максим 🌿  ")) }
    @Test fun emptyAndControlCharactersAreRejected() {
        for (value in listOf("", "   ", "A\nB", "A\tB")) assertFalse(Nickname.valid(value))
    }
    @Test fun lengthUsesUnicodeCodePoints() {
        assertTrue(Nickname.valid("🌿".repeat(32)))
        assertFalse(Nickname.valid("🌿".repeat(33)))
    }
    @Test fun restoreDoesNotInventAStoredName() {
        assertNull(Nickname.restore(null))
        assertNull(Nickname.restore("   "))
        assertEquals("Максим", Nickname.restore(" Максим "))
    }
}
