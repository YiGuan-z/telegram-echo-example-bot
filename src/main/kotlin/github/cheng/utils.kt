package github.cheng

import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.Path
import kotlin.properties.Delegates
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 *
 * @author caseycheng
 * @date 2023/10/2-13:42
 * @doc
 **/
fun envOfNullable(key: String): String? = System.getenv(key)

fun env(key: String): String = envOfNullable(key) ?: throw IllegalArgumentException("env $key not found")

inline fun <reified T : Any> Delegates.setOnce(): ReadWriteProperty<Any?, T> = SetOnce()

inline fun <reified T : Any> setOnce(): ReadWriteProperty<Any?, T> = SetOnce()

/**
 * 只能够写入一次属性
 */
class SetOnce<T : Any> : ReadWriteProperty<Any?, T> {
    private var value: T? = null
    private var isSet = false

    override fun getValue(
        thisRef: Any?,
        property: KProperty<*>,
    ): T = value ?: throw IllegalStateException("Property $property has not been initialized")


    override fun setValue(
        thisRef: Any?,
        property: KProperty<*>,
        value: T,
    ) =
        synchronized(this) {
            if (isSet) throw IllegalStateException("Property $property has already been initialized")
            this.value = value
            isSet = true
        }
}


internal fun ByteBuffer.getByte(): ByteArray {
    val byteArray = ByteArray(this.remaining())
    get(byteArray)
    return byteArray
}

fun packingZip(
    files: List<File>,
    outPutPath: Path,
    fileName: String,
    comment: String? = null,
    zipLevel: Int = 5
): File {
    files.forEach { file -> file.exists().takeIf { it } ?: throw IllegalArgumentException("$file not found") }
    val path = outPutPath.resolve("$fileName.zip")
    val zipFile = path.toFile().also { it.createNewFile() }
    zipFile.isFile.takeIf { it } ?: throw IllegalArgumentException("$path is a directory")
    val zipOutPutStream = ZipOutputStream(FileOutputStream(zipFile))
    zipOutPutStream.use { zipOutput ->
        comment?.let { comment -> zipOutput.setComment(comment) }
        zipOutput.setLevel(zipLevel)
        files.forEach { file ->
            file.inputStream().use { input ->
                zipOutput.putNextEntry(ZipEntry(file.name))
                input.copyTo(zipOutput)
                zipOutput.closeEntry()
            }
        }
        zipOutput.finish()
    }
    return zipFile
}

fun String.toPath() = Path(this)

@Suppress("unused")
fun String.toFile(): File = toPath().toFile()