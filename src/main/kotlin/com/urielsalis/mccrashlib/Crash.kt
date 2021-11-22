package com.urielsalis.mccrashlib

sealed class Crash {
    object Unknown : Crash()

    data class Minecraft(
        val modded: Boolean,
        /**
         * Whether the level had been opened with a modded client or server in the past. This does
         * not necessarily mean that the mod had any effect on the level, or that the client or
         * server is still modded (see [modded]).
         *
         * `null` if unknown, or if no level information exists in the crash report.
         */
        val wasLevelOpenedModded: Boolean?,
        val exception: String,
        /** Minecraft version ID; `null` if unknown */
        val minecraftVersion: String?,
        /** Whether this is a client crash (`true`) or a dedicated server crash (`false`) */
        val isClient: Boolean,
        val deobf: String?,
        val deobfException: String?
    ) : Crash()

    data class Jvm(
        /** Exception / OS signal causing the crash, e.g. `EXCEPTION_ACCESS_VIOLATION (0xc0000005) at ...`*/
        val exception: String?,
        /** Frame reported as 'problematic frame', causing the crash */
        val problematicFrame: JvmFrame?,
        /** Frames of native code stack trace, in the order in which they appear in the crash report (top to bottom) */
        val nativeFrames: List<JvmFrame>?,
        /** Frames of Java stack trace, in the order in which they appear in the crash report (top to bottom) */
        val javaFrames: List<JvmFrame>?,
        /** Minecraft version ID; `null` if unknown */
        val minecraftVersion: String?,
        /** Whether the crash report indicates that Minecraft is modded */
        val isModded: Boolean
    ) : Crash()

    // See also https://github.com/openjdk/jdk/blob/829dea45c9ab90518f03a66aad7e681cd4fda8b3/src/hotspot/share/runtime/frame.cpp#L569-L574
    sealed class JvmFrame {
        /** Frame in compiled Java code (type `J`) */
        data class JavaFrameCompiled(val location: String) : JvmFrame()
        /** Frame in interpreted Java code (type `j`) */
        data class JavaFrameInterpreted(val location: String) : JvmFrame()
        /** Frame in VM code (type `V`) */
        data class VmFrame(val location: String) : JvmFrame()
        /** Frame in VM generated code (type `v`) */
        data class VmGeneratedFrame(val location: String) : JvmFrame()
        /** Frame in C/C++ code (type `C`) */
        data class CFrame(
            /** The complete location information of the frame */
            val location: String,
            /** Name of the library, e.g. `ig75icd64.dll` */
            val libraryName: String?,
            /** Offset in the library, e.g. `0x1c82` */
            val libraryOffset: String?,
            /** Name of the function, e.g. `DrvSetLayerPaletteEntries` */
            val functionName: String?,
            /** Offset in the function, e.g. `0x96885` */
            val functionOffset: String?
        ) : JvmFrame()

        /** Frame type which could not be parsed as one of the other types */
        data class OtherFrame(
            /** The complete value of the frame, including the prefix char, if any */
            val value: String
        ) : JvmFrame()
    }

    object LauncherLog : Crash()
}
