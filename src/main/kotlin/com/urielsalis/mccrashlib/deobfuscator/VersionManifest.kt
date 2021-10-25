package com.urielsalis.mccrashlib.deobfuscator

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.net.URL

@JsonIgnoreProperties(ignoreUnknown = true)
data class LatestVersions(
    val release: String,
    val snapshot: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class VersionManifest(
    val latest: LatestVersions,
    val versions: List<Version>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Version(val id: String, val url: String)

fun fetchVersionManifest(): VersionManifest {
    val mapper = jacksonObjectMapper()
    return mapper.readValue(
        URL("https://launchermeta.mojang.com/mc/game/version_manifest.json"),
        VersionManifest::class.java
    )
}
