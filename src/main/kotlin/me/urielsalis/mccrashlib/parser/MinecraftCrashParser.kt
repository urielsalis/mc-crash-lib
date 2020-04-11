package me.urielsalis.mccrashlib.parser

import arrow.core.Either
import arrow.core.Option
import me.urielsalis.mccrashlib.Crash

/*
    Minecraft crashes are separated into sections starting and ending with --
    "Minecraft crash report" contains the exception, "System Details" contains more information
    The exception starts with java.lang and its all lines until the first empty line
    System details contains a : separated key value pair. We care about Is Modded
 */
const val crashReportSection = "Minecraft Crash Report"
const val systemDetailsSection = "System Details"
const val isModded = "Is Modded"

class MinecraftCrashParser : CrashParser {
    object SectionsNotFound : ParserError
    object NoExceptionFound : ParserError

    override fun parse(lines: List<String>): Either<ParserError, Crash> {
        val sections = parseSections(lines)
        if (!sections.containsKey(crashReportSection) || !sections.containsKey(systemDetailsSection)) {
            return Either.left(SectionsNotFound)
        }
        val details = getDetails(sections[systemDetailsSection] ?: error("System details section not found"))
        val exception = getException(sections[crashReportSection] ?: error("Crash report section not found"))

        return exception.fold(
            { Either.left(NoExceptionFound) },
            { Either.right(Crash.Minecraft(isModded(details), it)) }
        )
    }

    private fun isModded(details: Map<String, String>) =
        details.containsKey(isModded) && with(details[isModded] ?: error("Is Modded not found")) {
            !contains("Probably Not", true) && !contains("Unknown", true)
        }

    private fun getException(lines: List<String>): Option<String> {
        var foundStart = false
        val builder = StringBuilder()
        lines.map(String::trim).forEach {
            when {
                !foundStart -> if (
                    it.startsWith("java.lang") ||
                    it.startsWith("java.util") ||
                    it.startsWith("org.lwjgl")
                ) {
                    foundStart = true
                    builder.append(it)
                }
                else -> when {
                    it.isBlank() -> return Option.just(builder.toString())
                    else -> builder.append("\n" + it)
                }
            }
        }
        return Option.empty()
    }

    private fun getDetails(list: List<String>) = list
        .map(String::trim)
        .map {
            val pairs = it.split(":")
            pairs.first() to pairs.getOrElse(1) { "" }
        }
        .toMap()

    private fun parseSections(lines: List<String>): Map<String, List<String>> {
        val sections = mutableMapOf<String, List<String>>()
        val currentLines = mutableListOf<String>()
        var sectionName = ""
        lines.map(String::trim).forEach {
            if (it.startsWith("--") && it.endsWith("--")) {
                sections[sectionName] = currentLines.toList()
                currentLines.clear()
                sectionName = it.replace("-", "").trim()
            } else {
                currentLines.add(it)
            }
        }
        sections[sectionName] = currentLines
        return sections
    }
}
