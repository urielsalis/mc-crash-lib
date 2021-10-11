package com.urielsalis.mccrashlib

import arrow.core.Either
import com.urielsalis.mccrashlib.parser.JvmCrashParser
import com.urielsalis.mccrashlib.parser.MinecraftCrashParser
import com.urielsalis.mccrashlib.parser.ParserError
import java.io.File

const val MINECRAFT_CRASH_HEADER = "---- Minecraft Crash Report ----"
const val JVM_CRASH_HEADER = "#  EXCEPTION_ACCESS_VIOLATION"
const val SERVER_CONSOLE_LOG = "Client> "
const val LAUNCHER_LOG_CONTENT = "Launcher/launcher (main) Info"

class CrashReader {
    private val minecraftCrashParser = MinecraftCrashParser()
    private val jvmCrashParser = JvmCrashParser()

    fun processCrash(crash: List<String>, mappingsDir: File): Either<ParserError, Crash> = when {
        crash.anyContains(MINECRAFT_CRASH_HEADER) -> minecraftCrashParser.parse(crash, mappingsDir)
        crash.anyContains(SERVER_CONSOLE_LOG) && crash.anyContains(JVM_CRASH_HEADER) -> jvmCrashParser.parse(
            crash.map { it.replace(SERVER_CONSOLE_LOG, "").trim() },
            mappingsDir
        )
        crash.anyContains(JVM_CRASH_HEADER) -> jvmCrashParser.parse(crash, mappingsDir)
        crash.anyContains(LAUNCHER_LOG_CONTENT) -> Either.right(Crash.LauncherLog)
        else -> Either.right(Crash.Unknown)
    }
}

private fun List<String>.anyContains(str: String): Boolean = any { it.contains(str) }
