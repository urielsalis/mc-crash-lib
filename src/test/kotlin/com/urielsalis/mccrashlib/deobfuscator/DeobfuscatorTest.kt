package com.urielsalis.mccrashlib.deobfuscator

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
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

    @Test
    fun testGetDeobfuscation_UnknownVersion(@TempDir mappingsDir: File) {
        val e = assertThrows<IllegalArgumentException> {
            getDeobfuscation("does-not-exist", "crash content", true, mappingsDir)
        }
        assertEquals("Unknown version 'does-not-exist'", e.message)
    }

    @Test
    fun testGetDeobfuscation_MalformedVersion(@TempDir mappingsDir: File) {
        val e = assertThrows<IllegalArgumentException> {
            getDeobfuscation("../..", "crash content", true, mappingsDir)
        }
        assertEquals("Cannot create safe path with mappings file name '..?..-client.txt'", e.message)
    }

    @Test
    fun testGetDeobfuscation_VersionWithoutMappings(@TempDir mappingsDir: File) {
        val e = assertThrows<IllegalArgumentException> {
            // Version 19w35a has no deobfuscation mappings
            getDeobfuscation("19w35a", "crash content", true, mappingsDir)
        }
        assertEquals("Version 19w35a has no mappings", e.message)
    }
}
