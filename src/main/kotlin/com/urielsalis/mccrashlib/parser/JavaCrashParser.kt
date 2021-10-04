package com.urielsalis.mccrashlib.parser

import arrow.core.Either
import arrow.core.firstOrNone
import com.urielsalis.mccrashlib.Crash
import java.io.File

/*
    All the lines of the crash start with #
    The line we care about is
    # C  [xxxxxxxx+0x1c82]
    where we want to extract the xxxxxx
 */
class JavaCrashParser : CrashParser {
    object IncompleteJavaCrash : ParserError

    override fun parse(lines: List<String>, mappingsDirectory: File): Either<ParserError, Crash> {
        val linesWithMarker = lines.map(String::trim).filter { it.startsWith("#") }
        val importantLine = linesWithMarker.firstOrNone { it.startsWith("# C") && "[" in it && "+" in it }
        return importantLine.fold(
            { Either.left(IncompleteJavaCrash) },
            { Either.right(Crash.Java(
                isModded(lines),
                it.substring(it.indexOf('[') + 1, it.indexOf('+')))
            ) }
        )
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
