package com.urielsalis.mccrashlib.parser

import arrow.core.Either
import com.urielsalis.mccrashlib.Crash
import java.io.File

interface CrashParser {
    fun parse(lines: List<String>, mappingsDirectory: File): Either<ParserError, Crash>
}

interface ParserError
