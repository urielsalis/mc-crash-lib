import arrow.core.Either
import me.urielsalis.mccrashlib.Crash
import me.urielsalis.mccrashlib.CrashReader
import me.urielsalis.mccrashlib.parser.ParserError
import org.junit.Test
import java.io.File
import java.nio.file.Files

class CrashReaderIntegrationTest {
    val crashReader = CrashReader()
    val tempDir = Files.createTempDirectory("mappings").toFile()

    @Test
    fun shouldRunAllMinecraftCrashes() {
        val crashes = File(this::class.java.getResource("crashes/minecraft").file).listFiles()

        crashes.forEach {
            val name = it.name
            val parts = name.split("-")
            val isModded = parts[0] == "true"
            val expectedException = parts[1]

            val crashEither = crashReader.processCrash(it.readLines(), tempDir)
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

            val crashEither = crashReader.processCrash(it.readLines(), tempDir)
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
            val crashEither = crashReader.processCrash(it.readLines(), tempDir)
            assert(crashEither is Either.Right)
            val crash = (crashEither as Either.Right).b
            assert(crash is Crash.LauncherLog)
        }
    }

    @Test
    fun shouldProcessDeobfuscation() {
        val crashes = File(this::class.java.getResource("crashes/deobfuscator").file).listFiles()

        crashes.forEach {
            if (!it.nameWithoutExtension.endsWith("deobf")) {
                val either = crashReader.processCrash(it.readLines(), tempDir)
                val content = File(it.parent, it.nameWithoutExtension + "-deobf.log")
                if (content.exists()) {
                    assertDeobf(either, content.readText())
                } else {
                    assertDeobf(either, null)
                }
            }
        }
    }

    private fun assertDeobf(either: Either<ParserError, Crash>, content: String?) {
        assert(either is Either.Right)
        val crash = (either as Either.Right).b
        assert(crash is Crash.Minecraft)
        val minecraft = crash as Crash.Minecraft
        assert(minecraft.deobf?.dropLast(1) == content)
    }
}
