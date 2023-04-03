package com.urielsalis.mccrashlib.deobfuscator

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

internal class VersionManifestTest {
    @Test
    fun testFetchVersionManifest() {
        val manifest = fetchVersionManifest()
        val latestVersions = manifest.latest

        val latestRelease = manifest.versions.firstOrNull { it.id == latestVersions.release }
        assertNotNull(latestRelease)
        assertEquals(VersionType.RELEASE, latestRelease!!.type)

        val latestSnapshot = manifest.versions.firstOrNull { it.id == latestVersions.snapshot }
        assertNotNull(latestSnapshot)
        assertEquals(
            if (latestSnapshot == latestRelease) VersionType.RELEASE else VersionType.SNAPSHOT,
            latestSnapshot!!.type
        )

        // Pick one arbitrary release version
        val releaseVersion = manifest.versions.firstOrNull { it.id == "1.16.2" }
        assertEquals(
            Version(
                "1.16.2",
                VersionType.RELEASE,
                "https://piston-meta.mojang.com/v1/packages/a5cd0a3e52f38c9fb713010b07f7ae89e183b0ff/1.16.2.json",
                OffsetDateTime.parse("2022-02-25T13:15:31+00:00"),
                OffsetDateTime.parse("2020-08-11T10:13:46+00:00")
            ),
            releaseVersion
        )

        // Pick one arbitrary snapshot version
        val snapshotVersion = manifest.versions.firstOrNull { it.id == "1.16.2-rc2" }
        assertEquals(
            Version(
                "1.16.2-rc2",
                VersionType.SNAPSHOT,
                "https://piston-meta.mojang.com/v1/packages/0bea810bb372c8f44f2946b98288e298c48edd4d/1.16.2-rc2.json",
                OffsetDateTime.parse("2022-02-25T13:15:31+00:00"),
                OffsetDateTime.parse("2020-08-10T11:43:36+00:00")
            ),
            snapshotVersion
        )

        // Pick one arbitrary old-beta version
        val oldBetaVersion = manifest.versions.firstOrNull { it.id == "b1.5" }
        assertEquals(
            Version(
                "b1.5",
                VersionType.OLD_BETA,
                "https://piston-meta.mojang.com/v1/packages/3fa704bd73444368f04351d6d4add8a3eead9b4e/b1.5.json",
                OffsetDateTime.parse("2022-03-10T09:51:38+00:00"),
                OffsetDateTime.parse("2011-04-18T22:00:00+00:00")
            ),
            oldBetaVersion
        )

        // Pick one arbitrary old-alpha version
        val oldAlphaVersion = manifest.versions.firstOrNull { it.id == "rd-132328" }
        assertEquals(
            Version(
                "rd-132328",
                VersionType.OLD_ALPHA,
                "https://piston-meta.mojang.com/v1/packages/4ec49ff663f96e78a5cf0d9538adb9d1358fc485/rd-132328.json",
                OffsetDateTime.parse("2022-03-10T09:51:38+00:00"),
                OffsetDateTime.parse("2009-05-13T21:28:00+00:00")
            ),
            oldAlphaVersion
        )
    }
}
