import arrow.core.Either
import com.urielsalis.mccrashlib.Crash
import com.urielsalis.mccrashlib.CrashReader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.junit.jupiter.params.support.AnnotationConsumer
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Stream
import kotlin.streams.toList

class CrashReaderIntegrationTest {
    @Target(AnnotationTarget.FUNCTION)
    @Retention(AnnotationRetention.RUNTIME)
    @ArgumentsSource(ResourceFilesProvider::class)
    annotation class ResourceFilesSource(val directory: String, val ignoredNameSuffixes: Array<String> = [])

    class ResourceFilesProvider : ArgumentsProvider, AnnotationConsumer<ResourceFilesSource> {
        private lateinit var directory: String
        private lateinit var ignoredNameSuffixes: Array<String>

        override fun accept(t: ResourceFilesSource?) {
            directory = t!!.directory
            ignoredNameSuffixes = t.ignoredNameSuffixes
        }

        override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
            val resourceUrl = this::class.java.getResource(directory)
                ?: throw IllegalArgumentException("Resource directory '$directory' does not exist")

            return Files.list(Path.of(resourceUrl.toURI()))
                .filter(Files::isRegularFile)
                .map(Path::toFile)
                .filter { file -> !ignoredNameSuffixes.any { file.nameWithoutExtension.endsWith(it) } }
                .map(Arguments::of)
                // Files.list stream must be closed, therefore collect to list, then return stream
                .use { it.toList().stream() }
        }
    }

    private val crashReader = CrashReader()

    @TempDir
    lateinit var tempDir: File

    @ParameterizedTest
    @ResourceFilesSource("crashes/minecraft")
    fun shouldRunAllMinecraftCrashes(crashFile: File) {
        val name = crashFile.name
        val parts = name.split("-")
        val isModded = parts[0] == "true"
        val expectedException = parts[1]

        val crashEither = crashReader.processCrash(crashFile.readLines(), tempDir)
        assertTrue(crashEither is Either.Right)
        val crash = (crashEither as Either.Right).b
        assertTrue(crash is Crash.Minecraft)
        assertEquals(isModded, (crash as Crash.Minecraft).modded)
        assertTrue(crash.exception.contains(expectedException))
    }

    @ParameterizedTest
    @ResourceFilesSource("crashes/java")
    fun shouldRunAllJavaCrashes(crashFile: File) {
        val name = crashFile.name
        val parts = name.split("-")
        val isModded = parts[0] == "true"
        val expectedCode = parts[1]

        val crashEither = crashReader.processCrash(crashFile.readLines(), tempDir)
        assertTrue(crashEither is Either.Right)
        val crash = (crashEither as Either.Right).b
        assertTrue(crash is Crash.Java)
        assertEquals(isModded, (crash as Crash.Java).modded)
        assertEquals(expectedCode, crash.code)
    }

    @ParameterizedTest
    @ResourceFilesSource("crashes/log")
    fun shouldRunAllLogs(crashFile: File) {
        val crashEither = crashReader.processCrash(crashFile.readLines(), tempDir)
        assertTrue(crashEither is Either.Right)
        val crash = (crashEither as Either.Right).b
        assertTrue(crash is Crash.LauncherLog)
    }

    /** Removes either a trailing `\r\n`, `\n` or `\r`. */
    private fun String.dropLastLineTerminator() = removeSuffix("\n").removeSuffix("\r")

    @ParameterizedTest
    @ResourceFilesSource("crashes/deobfuscator", ignoredNameSuffixes = ["deobf"])
    fun shouldProcessDeobfuscation(crashFile: File) {
        val either = crashReader.processCrash(crashFile.readLines(), tempDir)
        val deobfContent = File(crashFile.parent, crashFile.nameWithoutExtension + "-deobf.txt")
            .takeIf(File::isFile)?.readText()

        assertTrue(either is Either.Right)
        val crash = (either as Either.Right).b
        assertTrue(crash is Crash.Minecraft)
        val minecraft = crash as Crash.Minecraft
        assertEquals(deobfContent, minecraft.deobf?.dropLastLineTerminator())
    }
}
