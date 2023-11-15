package github.cheng.module.bot.modal

import kotlin.io.path.Path
import kotlin.io.path.createDirectories

/**
 *
 * @author caseycheng
 * @date 2023/10/17-12:24
 * @doc
 **/
data class Fpath(
    val packPath: String,
    val srcPath: String,
    val imgPath: String,
) {
    constructor(packPath: String) : this(packPath, "$packPath/src/", "$packPath/img/")

    fun mkdirFinder() {
        Path(packPath).toAbsolutePath().createDirectories()
        Path(srcPath).toAbsolutePath().createDirectories()
        Path(imgPath).toAbsolutePath().createDirectories()
    }
}
