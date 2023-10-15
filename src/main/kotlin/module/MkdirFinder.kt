package module

import application.Application
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes

/**
 *
 * @author caseycheng
 * @date 2023/10/14-17:44
 * @doc
 **/
fun Application.mkdirImageFinder() {
    val images = appEnvironment.config("bot.images")
    val fileStorage = images.property("file_storage").getString()
    val fsPath = Paths.get(fileStorage).toAbsolutePath()
    try {
        Files.readAttributes(fsPath, BasicFileAttributes::class.java)
    } catch (e: IOException) {
        logger.info("[internal] File storage not found, creating...")
        Files.createDirectories(fsPath)
    }


}