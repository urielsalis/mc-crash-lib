package com.urielsalis.mccrashlib.deobfuscator

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import java.net.URL
import java.time.OffsetDateTime

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

// Note: Could use Jackson's `DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL`
// in the future to protect against deserialization failure when new types are added
enum class VersionType {
    @JsonProperty("release")
    RELEASE,
    @JsonProperty("snapshot")
    SNAPSHOT,
    @JsonProperty("old_beta")
    OLD_BETA,
    @JsonProperty("old_alpha")
    OLD_ALPHA
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class Version(
    val id: String,
    val type: VersionType,
    val url: String,
    val time: OffsetDateTime,
    val releaseTime: OffsetDateTime
)

fun fetchVersionManifest(): VersionManifest {
    val mapper = jacksonMapperBuilder()
        .addModule(JavaTimeModule())
        .build()
    return mapper.readValue(
        URL("https://launchermeta.mojang.com/mc/game/version_manifest.json"),
        VersionManifest::class.java
    )
}
