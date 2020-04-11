import arrow.core.Either
import me.urielsalis.mccrashlib.Crash
import me.urielsalis.mccrashlib.CrashReader
import org.junit.Test
import java.io.File

class CrashReaderIntegrationTest {
    val crashReader = CrashReader()

    @Test
    fun shouldRunAllMinecraftCrashes() {
        val crashes = File(this::class.java.getResource("crashes/minecraft").file).listFiles()

        crashes.forEach {
            val name = it.name
            val parts = name.split("-")
            val isModded = parts[0] == "true"
            val expectedException = parts[1]

            val crashEither = crashReader.processCrash(it.readLines())
            assert(crashEither is Either.Right)
            val crash = (crashEither as Either.Right).b
            assert(crash is Crash.Minecraft)
            assert((crash as Crash.Minecraft).modded == isModded)
            assert(crash.exception.contains(expectedException))
        }
    }

    @Test
    fun shouldRunAllJavaCrashes() {
        val crashes = File(this::class.java.getResource("crashes/java").file).listFiles()

        crashes.forEach {
            val name = it.name
            val parts = name.split("-")
            val expectedCode = parts[0]

            val crashEither = crashReader.processCrash(it.readLines())
            assert(crashEither is Either.Right)
            val crash = (crashEither as Either.Right).b
            assert(crash is Crash.Java)
            assert((crash as Crash.Java).code == expectedCode)
        }
    }

    @Test
    fun shouldRunAllLogs() {
        val crashes = File(this::class.java.getResource("crashes/log").file).listFiles()

        crashes.forEach {
            val crashEither = crashReader.processCrash(it.readLines())
            assert(crashEither is Either.Right)
            val crash = (crashEither as Either.Right).b
            assert(crash is Crash.LauncherLog)
        }
    }
}
