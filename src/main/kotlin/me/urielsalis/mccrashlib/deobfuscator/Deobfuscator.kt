package me.urielsalis.mccrashlib.deobfuscator

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import proguard.retrace.ReTrace
import java.io.File
import java.io.IOException
import java.io.LineNumberReader
import java.io.PrintWriter
import java.io.StringReader
import java.io.StringWriter
import java.net.URI
import java.net.URL
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

private fun getMappingFile(mappingsDirectory: File, version: String, isClient: Boolean): File? {
    // Reject malformed or malicious names
    if (version.isBlank() || version.contains("\\") || version.contains("/")) {
        return null
    }

    val name = if (isClient) {
        "$version-client.txt"
    } else {
        "$version-server.txt"
    }

    val canonicalDestinationDir = mappingsDirectory.canonicalPath
    val destinationFile = File(mappingsDirectory, name)
    val canonicalDestinationFile = destinationFile.canonicalPath
    // Reject if file would be outside intended directory
    // Based on Zip Slip mitigation, see https://snyk.io/research/zip-slip-vulnerability#java
    if (!canonicalDestinationFile.startsWith(canonicalDestinationDir + File.separator)) {
        return null
    }
    return destinationFile
}

fun getDeobfuscation(
    modded: Boolean,
    version: String,
    content: String,
    isClient: Boolean,
    mappingsDirectory: File
): String? {
    if (modded) {
        return null
    }

    val mappingFile = getMappingFile(mappingsDirectory, version, isClient) ?: return null

    // Synchronize to prevent race condition while downloading mapping file
    synchronized(mappingsDirectory) {
        if (!mappingFile.exists()) {
            downloadMapping(version, mappingFile, isClient)
        }
        if (!mappingFile.exists()) {
            return null
        }
    }
    val retrace = ReTrace(ReTrace.REGULAR_EXPRESSION, false, true, mappingFile)
    val stringWriter = StringWriter()
    val printWriter = PrintWriter(stringWriter)
    retrace.retrace(LineNumberReader(StringReader(content)), printWriter)
    printWriter.flush()
    return stringWriter.toString()
}

private fun downloadMapping(version: String, mappingFile: File, isClient: Boolean) {
    val mapper = jacksonObjectMapper()
    val manifest = mapper
        .readValue(URL("https://launchermeta.mojang.com/mc/game/version_manifest.json"), VersionManifest::class.java)
    val url = manifest.versions.firstOrNull { it.id == version }?.url ?: return
    val versionInfo = mapper.readValue(URL(url), VersionInfo::class.java)
    val mappingUrl = if (isClient) {
        versionInfo.downloads.clientMappings?.url
    } else {
        versionInfo.downloads.serverMappings?.url
    }
    if (mappingUrl == null) {
        return
    }
    downloadFile(mappingUrl, mappingFile)
}

fun downloadFile(url: String, file: File) {
    val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(2))
        .build()
    val request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .build()
    val bodyHandler = HttpResponse.BodyHandler { responseInfo ->
        val statusCode = responseInfo.statusCode()
        if (statusCode == 200) {
            HttpResponse.BodySubscribers.ofFile(file.toPath())
        } else {
            HttpResponse.BodySubscribers.discarding()
        }
    }

    val response = try {
        client.send(request, bodyHandler)
    } catch (ioException: IOException) {
        // Delete the file (in case it exists) since it is likely incomplete
        file.delete()
        throw ioException
    }

    if (response.body() == null) {
        throw IOException("Request to $url failed with HTTP status code ${response.statusCode()}")
    }
}
