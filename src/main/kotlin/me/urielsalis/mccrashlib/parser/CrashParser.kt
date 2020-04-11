package me.urielsalis.mccrashlib.parser

import arrow.core.Either
import me.urielsalis.mccrashlib.Crash

interface CrashParser {
    fun parse(lines: List<String>): Either<ParserError, Crash>
}

interface ParserError