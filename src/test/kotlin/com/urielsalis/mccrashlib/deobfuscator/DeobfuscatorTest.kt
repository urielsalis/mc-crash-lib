package com.urielsalis.mccrashlib.deobfuscator

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.File

class DeobfuscatorTest {
    @ParameterizedTest
    @ValueSource(strings = ["", "/", "/test", "test/", "\\test", "..", ".", "../../test", "test/../"])
    fun testInvalidNames(childName: String) {
        val dir = File("dir")
        assertNull(getSafeChildPath(dir, childName))
    }

    @ParameterizedTest
    @ValueSource(strings = ["test", "test.txt", "test.", ".test", "test.."])
    fun testValidNames(childName: String) {
        val dir = File("dir")
        assertEquals(File(dir, childName), getSafeChildPath(dir, childName))
    }
}
