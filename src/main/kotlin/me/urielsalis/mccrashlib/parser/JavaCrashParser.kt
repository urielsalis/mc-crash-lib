package me.urielsalis.mccrashlib.parser

import arrow.core.Either
import arrow.core.firstOrNone
import me.urielsalis.mccrashlib.Crash
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
            { Either.right(Crash.Java(it.substring(it.indexOf('[') + 1, it.indexOf('+')))) }
        )
    }
}
