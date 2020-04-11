package me.urielsalis.mccrashlib

import arrow.core.Either
import me.urielsalis.mccrashlib.parser.JavaCrashParser
import me.urielsalis.mccrashlib.parser.MinecraftCrashParser
import me.urielsalis.mccrashlib.parser.ParserError

const val MINECRAFT_CRASH_HEADER = "---- Minecraft Crash Report ----"
const val JAVA_CRASH_HEADER = "#  EXCEPTION_ACCESS_VIOLATION"
const val SERVER_CONSOLE_LOG = "Client> "
const val LAUNCHER_LOG_CONTENT = "Launcher/launcher (main) Info"

class CrashReader {
    val minecraftParser = MinecraftCrashParser()
    val javaParser = JavaCrashParser()

    fun processCrash(crash: List<String>): Either<ParserError, Crash> = when {
        crash.anyContains(MINECRAFT_CRASH_HEADER) -> minecraftParser.parse(crash)
        crash.anyContains(SERVER_CONSOLE_LOG) && crash.anyContains(JAVA_CRASH_HEADER) -> javaParser.parse(
            crash.map { it.replace(SERVER_CONSOLE_LOG, "").trim() }
        )
        crash.anyContains(JAVA_CRASH_HEADER) -> javaParser.parse(crash)
        crash.anyContains(LAUNCHER_LOG_CONTENT) -> Either.right(Crash.LauncherLog)
        else -> Either.right(Crash.Unknown)
    }
}

private fun List<String>.anyContains(str: String): Boolean = any { it.contains(str) }
