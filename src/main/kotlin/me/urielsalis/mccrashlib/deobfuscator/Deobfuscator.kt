package me.urielsalis.mccrashlib.deobfuscator

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import proguard.retrace.ReTrace
import java.io.File
import java.io.FileOutputStream
import java.io.LineNumberReader
import java.io.PrintWriter
import java.io.StringReader
import java.io.StringWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files

val mappingsFile = Files.createTempDirectory("mappings").toFile()

fun getDeobfuscation(modded: Boolean, version: String, content: String, isClient: Boolean): String? {
    if (modded) {
        return null
    }

    if (version.isBlank() || version.contains("\\") || version.contains("/") || isZipSlip(version)) {
        return null
    }

    val name = if (isClient) {
        "$version-client"
    } else {
        "$version-server"
    }

    val mappingFile = synchronized(mappingsFile) {
        val mappingFile = File(mappingsFile, name)
        if (!mappingFile.exists()) {
            downloadMapping(version, name, isClient)
        }
        if (!mappingFile.exists()) {
            return null
        }
        mappingFile
    }
    val retrace = ReTrace(ReTrace.REGULAR_EXPRESSION, false, true, mappingFile)
    val stringWriter = StringWriter()
    val printWriter = PrintWriter(stringWriter)
    retrace.retrace(LineNumberReader(StringReader(content)), printWriter)
    return stringWriter.toString()
}

fun isZipSlip(version: String): Boolean {
    val canonicalDestinationDir = mappingsFile.canonicalPath
    val destinationFile = File(mappingsFile, version)
    val canonicalDestinationFile = destinationFile.canonicalPath
    return canonicalDestinationFile.startsWith(canonicalDestinationDir + File.separator)
}

private fun downloadMapping(version: String, name: String, isClient: Boolean) {
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
    downloadFile(mappingUrl, name)
}

fun downloadFile(url: String, name: String) {
    val connection: HttpURLConnection = URL(url).openConnection() as HttpURLConnection
    connection.requestMethod = "GET"
    val stream = connection.inputStream
    val file = File(mappingsFile, name)
    file.createNewFile()
    val out = FileOutputStream(file)
    stream.transferTo(out)
    out.flush()
    out.close()
    connection.disconnect()
}
