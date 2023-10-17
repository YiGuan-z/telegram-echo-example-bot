package module.bot

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.Path
import kotlin.io.path.name
import kotlin.io.path.pathString

/**
 *
 * @author caseycheng
 * @date 2023/10/15-21:45
 * @doc
 **/
/**
 * 解析参数，将参数解析为Map
 */
fun List<String>.resolveArgs(): Map<String, String> {
    val result: MutableMap<String, String> = mutableMapOf()
    var key = ""
    this.forEachIndexed { index, arg ->
        if (index % 2 == 0) {
            key = arg
        } else {
            result[key] = arg
        }
    }
    return result
}

fun Path.basename() = this.fileName.name

fun String.suffix() = this.drop(this.lastIndexOf('.') + 1)

fun copyFile(sourceFile: Path, destFile: Path) {
    Files.createDirectories(destFile.parent)

    Files.copy(sourceFile, destFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES)
}

fun currentPath() = Path("").toAbsolutePath().pathString
