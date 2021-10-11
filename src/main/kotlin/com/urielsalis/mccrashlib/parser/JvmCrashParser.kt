package com.urielsalis.mccrashlib.parser

import arrow.core.Either
import com.urielsalis.mccrashlib.Crash
import java.io.File

// https://github.com/openjdk/jdk/blob/829dea45c9ab90518f03a66aad7e681cd4fda8b3/src/hotspot/share/utilities/vmError.cpp#L537
internal const val JVM_CRASH_HEADER = "# A fatal error has been detected by the Java Runtime Environment:"

// https://github.com/openjdk/jdk/blob/829dea45c9ab90518f03a66aad7e681cd4fda8b3/src/hotspot/share/utilities/vmError.cpp#L700
private const val PROBLEMATIC_FRAME_MARKER = "# Problematic frame:"
// https://github.com/openjdk/jdk/blob/829dea45c9ab90518f03a66aad7e681cd4fda8b3/src/hotspot/share/utilities/vmError.cpp#L236
private const val JAVA_STACK_TRACE_MARKER = "Java frames: (J=compiled Java code, j=interpreted, Vv=VM code)"

private val NATIVE_STACK_TRACE_MARKERS = listOf(
    // https://github.com/openjdk/jdk/blob/829dea45c9ab90518f03a66aad7e681cd4fda8b3/src/hotspot/share/utilities/vmError.cpp#L350
    "Native frames: (J=compiled Java code, j=interpreted, Vv=VM code, C=native code)",
    // Before JDK-8264805
    // https://github.com/openjdk/jdk/blob/15d4787724ad8723d36e771a9709db51933df2c1/src/hotspot/share/utilities/vmError.cpp#L245
    "Native frames: (J=compiled Java code, A=aot compiled Java code, j=interpreted, Vv=VM code, C=native code)"
)

class JvmCrashParser : CrashParser {
    /** If present removes the prefix, otherwise returns `null` */
    private fun String.removeRequiredPrefix(prefix: String): String? {
        return if (startsWith(prefix)) substring(prefix.length) else null
    }

    private fun parseFrame(frameLine: String): Crash.JvmFrame {
        if (frameLine.length >= 2 && frameLine[1] == ' ') {
            val frameTypeChar = frameLine[0]
            // Use `trimStart()` because some frames have two spaces between type char and location
            val frameLocation = frameLine.substring(2).trimStart()

            // https://github.com/openjdk/jdk/blob/829dea45c9ab90518f03a66aad7e681cd4fda8b3/src/hotspot/share/runtime/frame.cpp#L569-L574
            return when (frameTypeChar) {
                // Not parsing most of these because they are not relevant for Arisa
                'J' -> Crash.JvmFrame.JavaFrameCompiled(frameLocation)
                'j' -> Crash.JvmFrame.JavaFrameInterpreted(frameLocation)
                'V' -> Crash.JvmFrame.VmFrame(frameLocation)
                'v' -> Crash.JvmFrame.VmGeneratedFrame(frameLocation)
                'C' -> parseCFrame(frameLocation)
                else -> Crash.JvmFrame.OtherFrame(frameLine)
            }
        }
        return Crash.JvmFrame.OtherFrame(frameLine)
    }

    // https://github.com/openjdk/jdk/blob/829dea45c9ab90518f03a66aad7e681cd4fda8b3/src/hotspot/share/runtime/frame.cpp#L536
    /**
     * Groups:
     *   1. library name
     *   2. library offset
     *   3. function name (optional)
     *   4. function offset (optional)
     *
     * Only tries to parse function name and offset when library location was parsed successfully;
     * otherwise it might be ambiguous.
     */
    private val C_FRAME_REGEX = Regex("""\[(.+)\+(.+)\](?:  (.+)\+(.+))?""")
    private fun parseCFrame(frameLocation: String): Crash.JvmFrame.CFrame {
        var libraryName: String? = null
        var libraryOffset: String? = null
        var functionName: String? = null
        var functionOffset: String? = null

        C_FRAME_REGEX.matchEntire(frameLocation)?.groups?.apply {
            libraryName = get(1)?.value
            libraryOffset = get(2)?.value
            functionName = get(3)?.value
            functionOffset = get(4)?.value
        }

        return Crash.JvmFrame.CFrame(frameLocation, libraryName, libraryOffset, functionName, functionOffset)
    }

    private fun parseStackTrace(lines: List<String>, marker: String): List<Crash.JvmFrame>? {
        val markerIndex = lines.indexOf(marker)
        if (markerIndex == -1) {
            return null
        }

        val stackTrace = mutableListOf<Crash.JvmFrame>()
        for (i in markerIndex + 1 until lines.size) {
            val line = lines[i]
            if (line.isBlank()) {
                break
            }

            stackTrace.add(parseFrame(line))
        }
        return stackTrace
    }

    private fun <E> List<E>.indexOf(e: E, startIndex: Int): Int {
        val i = subList(startIndex, size).indexOf(e)
        return if (i == -1) i else startIndex + i
    }

    override fun parse(lines: List<String>, mappingsDirectory: File): Either<ParserError, Crash> {
        val trimmedLines = lines.map(String::trim)

        val crashHeaderIndex = trimmedLines.indexOf(JVM_CRASH_HEADER)
        if (crashHeaderIndex == -1) {
            throw IllegalArgumentException("Not a JVM crash")
        }

        // Find next line containing only '#' followed by line containing exception
        // https://github.com/openjdk/jdk/blob/829dea45c9ab90518f03a66aad7e681cd4fda8b3/src/hotspot/share/utilities/vmError.cpp#L643-L644
        var exception: String? = null
        val nextEmptyIndex = lines.indexOf("#", crashHeaderIndex)
        if (nextEmptyIndex != -1) {
            exception = lines.getOrNull(nextEmptyIndex + 1)?.removeRequiredPrefix("#  ")
        }

        val frameMarkerIndex = trimmedLines.indexOf(PROBLEMATIC_FRAME_MARKER)
        var frame: Crash.JvmFrame? = null

        if (frameMarkerIndex != -1) {
            val frameLine = trimmedLines.getOrNull(frameMarkerIndex + 1)?.removeRequiredPrefix("# ")
            frame = frameLine?.let(::parseFrame)
        }

        return Either.right(Crash.Jvm(
            exception,
            frame,
            // Find first matching marker
            NATIVE_STACK_TRACE_MARKERS.asSequence()
                .mapNotNull { marker -> parseStackTrace(trimmedLines, marker) }
                .firstOrNull(),
            parseStackTrace(trimmedLines, JAVA_STACK_TRACE_MARKER),
            isModded(lines)
        ))
    }

    private fun isModded(lines: List<String>): Boolean {
        val moddedStrings = listOf(
            // Note: Checking for any occurrence of "--tweakClass" might be error-prone in case
            // vanilla uses it as well
            "--tweakClass optifine.OptiFineTweaker",
            "net.fabricmc.loader.launch.knot.KnotClient",
            "net.fabricmc.loader.impl.launch.knot.KnotClient", // new class name
            "--version fabric-loader-"
        )
        return lines.any { moddedStrings.any(it::contains) }
    }
}
