package com.urielsalis.mccrashlib

sealed class Crash {
    object Unknown : Crash()
    data class Minecraft(
        val modded: Boolean,
        val exception: String,
        /** Minecraft version ID; `null` if unknown */
        val minecraftVersion: String?,
        /** Whether this is a client crash (`true`) or a dedicated server crash (`false`) */
        val isClient: Boolean,
        val deobf: String?,
        val deobfException: String?
    ) : Crash()

    data class Java(
        val modded: Boolean,
        val code: String
    ) : Crash()
    object LauncherLog : Crash()
}
