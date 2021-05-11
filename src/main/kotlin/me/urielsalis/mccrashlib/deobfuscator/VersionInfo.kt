package me.urielsalis.mccrashlib.deobfuscator

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class VersionInfo(
    val downloads: Downloads
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Downloads(
    @JsonProperty("client_mappings")
    val clientMappings: Download?,
    @JsonProperty("server_mappings")
    val serverMappings: Download?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Download(val url: String)
