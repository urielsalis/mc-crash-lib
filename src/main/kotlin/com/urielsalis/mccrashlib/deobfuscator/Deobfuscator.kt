package com.urielsalis.mccrashlib.deobfuscator

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

private val sanitizationRegex = Regex("[^a-zA-Z0-9\\-+_#*?.,; ]")
private fun sanitize(arg: String): String {
    return arg.replace(sanitizationRegex, "?")
}

// Note: Also used by Arisa (https://github.com/mojira/arisa-kt)
/**
 * Gets the path of a file or directory called `childName` within the `directory`.
 * If the child name is malformed (e.g. empty) or malicious and would result in a location outside the intended
 * directory, `null` is returned.
 *
 * @param directory
 *      Parent directory
 * @param childName
 *      Name of the child file or directory in `directory`
 * @return
 *      Path of the child, or `null` if the path would not be safe
 */
fun getSafeChildPath(directory: File, childName: String): File? {
    // Reject malformed or malicious names
    if (childName.isBlank() || childName.contains("\\") || childName.contains("/")) {
        return null
    }

    val canonicalDestinationDir = directory.canonicalPath
    val destinationFile = File(directory, childName)
    val canonicalDestinationFile = destinationFile.canonicalPath
    // Reject if file would be outside intended directory
    // Based on Zip Slip mitigation, see https://snyk.io/research/zip-slip-vulnerability#java
    if (!canonicalDestinationFile.startsWith(canonicalDestinationDir + File.separator)) {
        return null
    }
    return destinationFile
}

private fun getMappingFile(mappingsDirectory: File, version: String, isClient: Boolean): File {
    val name = if (isClient) {
        "$version-client.txt"
    } else {
        "$version-server.txt"
    }

    return getSafeChildPath(mappingsDirectory, name)
        // Sanitize `name` to avoid injection of untrusted text into exception message
        ?: throw IllegalArgumentException("Cannot create safe path with mappings file name '${sanitize(name)}'")
}

/**
 * @param version
 *      Minecraft version ID
 * @param content
 *      Content to deobfuscate
 * @param isClient
 *      Whether client (`true`) or dedicated server (`false`) mappings should be used for deobfuscation
 * @param mappingsDirectory
 *      Existing directory where to load and store downloaded mappings
 * @return
 *      The deobfuscated content
 * @throws IllegalArgumentException
 *      If no mappings exist for the version
 */
fun getDeobfuscation(
    version: String,
    content: String,
    isClient: Boolean,
    mappingsDirectory: File
): String {
    val mappingFile = getMappingFile(mappingsDirectory, version, isClient)

    // Synchronize to prevent race condition while downloading mapping file
    synchronized(mappingsDirectory) {
        if (!mappingFile.exists()) {
            downloadMapping(version, mappingFile, isClient)
        }
    }
    // Matches <whitespace>at<whitespace>(modulename)//(className).(method)((sourcefile):(line))
    val java16Regex = "(?:.*?\\bat\\s+[a-zA-Z]*\\//)%c\\.%m\\(%s:%l\\)"
    // Matches <whitespace>at<whitespace>(optional module name//)(className)$$Lambda$(anything).(method)(sourcefile)
    val lambdaRegex = "(?:.*?\\bat\\s+(?:[a-zA-Z]*\\/\\/)?)%c\\\$\\\$Lambda\\\$\\d*\\/(.*)\\.%m\\((.*)\\)"
    val regex = ReTrace.REGULAR_EXPRESSION + "|(?:" + java16Regex + ")|(?:" + lambdaRegex + ")"
    val retrace = ReTrace(regex, false, true, mappingFile)
    val stringWriter = StringWriter()
    val printWriter = PrintWriter(stringWriter)
    retrace.retrace(LineNumberReader(StringReader(content)), printWriter)
    printWriter.flush()
    return stringWriter.toString()
}

/**
 * @throws IllegalArgumentException if no mappings exist for the version
 */
private fun downloadMapping(version: String, mappingFile: File, isClient: Boolean) {
    val manifest = fetchVersionManifest()
    val url = manifest.versions.firstOrNull { it.id == version }?.url
        // Sanitize `version` to avoid injection of untrusted text into exception message
        ?: throw IllegalArgumentException("Unknown version '${sanitize(version)}'")
    val mapper = jacksonObjectMapper()
    val versionInfo = mapper.readValue(URL(url), VersionInfo::class.java)
    val mappingUrl = if (isClient) {
        versionInfo.downloads.clientMappings?.url
    } else {
        versionInfo.downloads.serverMappings?.url
    }
    if (mappingUrl == null) {
        throw IllegalArgumentException("Version $version has no mappings")
    }
    downloadFile(mappingUrl, mappingFile)
}

private fun downloadFile(url: String, file: File) {
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
