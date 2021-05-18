package com.urielsalis.mccrashlib.deobfuscator

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class VersionManifest(
    val versions: List<Version>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Version(val id: String, val url: String)
