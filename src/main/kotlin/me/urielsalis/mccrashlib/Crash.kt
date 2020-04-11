package me.urielsalis.mccrashlib

sealed class Crash {
    object Unknown : Crash()
    data class Minecraft(val modded: Boolean, val exception: String, val minecraftVersion: String) : Crash()
    data class Java(val code: String) : Crash()
    object LauncherLog : Crash()
}
