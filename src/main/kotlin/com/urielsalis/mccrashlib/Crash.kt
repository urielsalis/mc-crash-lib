package com.urielsalis.mccrashlib

sealed class Crash {
    object Unknown : Crash()
    data class Minecraft(
        val modded: Boolean,
        val exception: String,
        val minecraftVersion: String,
        val deobf: String?,
        val deobfException: String?
    ) : Crash()

    data class Java(val code: String) : Crash()
    object LauncherLog : Crash()
}
