import java.io.ByteArrayInputStream
import java.io.File
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

fun decompressJar(jarFile: File) {
    // This really shouldn't be in-place, but it's convenient and the jars are small anyway.
    val bytes = jarFile.readBytes()
    ZipInputStream(ByteArrayInputStream(bytes)).use { input ->
        val entries = generateSequence { input.nextEntry }
        ZipOutputStream(jarFile.outputStream()).use { output ->
            output.setLevel(Deflater.NO_COMPRESSION)
            for (entry in entries) {
                output.putNextEntry(ZipEntry(entry))
                output.write(input.readBytes())
                output.closeEntry()
            }
        }
    }
}