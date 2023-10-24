package module.bot

import GlobalResource
import application.*
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.handlers.CommandHandlerEnvironment
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.TelegramFile
import kotlinx.coroutines.*
import module.bot.modal.ChatLangProfile
import module.bot.modal.Fpath
import module.bot.modal.StickerCollectPack
import module.currentChatId
import module.opencv.OpenCVService
import module.redis.RedisService
import module.redis.currentChatLangProfile
import module.thisLogger
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.deleteRecursively
import kotlin.io.path.pathString

/**
 *
 * @author caseycheng
 * @date 2023/10/15-15:27
 * @doc 可以依赖[ChatLangProfile]对象，但是不能在内部加字段，在redis中开辟一片新内存也是可以的。
 **/
val newPackCommand = createBotDispatcherModule("newPackCommand", ::NewPackCommandConfiguration) { config ->
    val redisService = requireNotNull(config.redisService) { "need redisService" }
    val i18n = requireNotNull(config.i18nPacks) { "need i18nPacks" }
    val zipCommand = requireNotNull(config.zipCommand) { "need zipCommand" }
    NewPackCommand(redisService, i18n, zipCommand)
}

class NewPackCommandConfiguration {
    var redisService: RedisService? = null
    var i18nPacks: I18nPacks? = null
    var zipCommand: String? = null
}

@BotDSL
fun NewPackCommandConfiguration.setRedisService(redisService: RedisService) {
    this.redisService = redisService
}

@BotDSL
fun NewPackCommandConfiguration.setI18n(i18nPacks: I18nPacks) {
    this.i18nPacks = i18nPacks
}

class NewPackCommand(
    private val redisService: RedisService,
    private val i18n: I18nPacks,
    private val zipCommand: String
) : BotDispatcher {
    private val logger = thisLogger<NewPackCommand>()
    override val dispatch: Dispatcher.() -> Unit = {
        newPackCommand()
        finish()
    }

    private fun Dispatcher.newPackCommand() {
        command("newpack") {
            val chatId = currentChatId()
            val langProfile = redisService.currentChatLangProfile(chatId) ?: return@command
            val languagePack = i18n.get(langProfile.lang)
            val pack = redisService.getCurrentPack(chatId)
            if (pack == null) {
                //message.date 中是一个秒级时间戳
                redisService.setCurrentPack(chatId, StickerCollectPack(message.date))
                bot.sendMessage(
                    chatId,
                    languagePack.getString("newpack.newpack", "max", GlobalResource.maxImages.toString())
                )
                return@command
            }
            pack.let {
                if (pack.files.isNotEmpty()) {
                    bot.sendMessage(chatId, languagePack.getString("newpack.taskexist"))
                } else {
                    bot.sendMessage(chatId, languagePack.getString("newpack.tasklocked"))
                }
                return@command
            }
        }
    }

    private fun Dispatcher.finish() {
        command("finish") {
            val chatId = currentChatId()
            val langProfile = redisService.currentChatLangProfile(chatId) ?: return@command
            val languagePack = i18n.get(langProfile.lang)
            try {
                val stickerCollectPack = redisService.getCurrentPack(chatId)!!
                stickerCollectPack.let {
                    if (it.isLocked) {
                        bot.sendMessage(chatId, languagePack.getString("newpack.tasklocked"))
                        return@command
                    }
                    val map = args.toMap()
                    val format = map["-format"]
                    val width = map["width"]

                    if (stickerCollectPack.files.isNotEmpty()) {
                        finishHandler(format, width, stickerCollectPack, languagePack)
                    } else {
                        bot.sendMessage(chatId, languagePack.getString("nosticker"))
                    }
                    return@command
                }
            } catch (ignore: Exception) {
                bot.sendMessage(chatId, languagePack.getString("error.user_prompt", "user", GlobalResource.adminName))
                logger.error("[finish command] get a error chat id is ${chatId.id} and error is", ignore)
                return@command
            }

        }
    }

    /**
     * 从[StickerCollectPack]中创建任务
     */
    private suspend fun CommandHandlerEnvironment.finishHandler(
        format: String?,
        width: String?,
        stickerCollectPack: StickerCollectPack,
        langPack: LanguagePack
    ) {
        val chatId = currentChatId()
        val collectPack = stickerCollectPack.copy(isLocked = true)
        logger.info("[Pack Task] chatId ${chatId.id} Starting pack task...")
        redisService.setCurrentPack(chatId, collectPack)
        val packPath = "${GlobalResource.imageStorage}/${chatId.id}"

        val fpath = Fpath(
            packPath,
        )

        fpath.mkdirFinder()

        coroutineScope {
            withContext(Dispatchers.IO) {
                val job1 = async {
                    // 下载贴纸
                    bot.sendMessage(chatId, langPack.getString("downloadstep.downloading"))
                    downloadHandler(fpath, stickerCollectPack, langPack)
                }
                val job2 = async {
                    job1.await()
                    //转换为对应格式
                    bot.sendMessage(chatId, langPack.getString("downloadstep.converting"))
                    convertHandler(fpath, format, width, langPack)
                }
                val job3 = async {
                    job2.await()
                    //📦打包贴纸
                    bot.sendMessage(chatId, langPack.getString("downloadstep.packaging"))
                    zipHandler()
                }
                val zipFile = job3.await()
                //发送贴纸
                bot.sendMessage(chatId, langPack.getString("downloadstep.sending"))
                logger.info("[finish command] chat ${chatId.id} sending zip file...")
                Path(zipFile).toFile().let {
                    if (it.isFile) {
                        val isEmpty = runCatching { ZipFile(it).size() == 0 }.getOrNull() ?: true
                        if (isEmpty) {
                            bot.sendMessage(chatId, langPack.getString("sticker.oops_not_file"))
                            logger.info("[finish command] chat ${chatId.id} oops this zip is null")
                        } else {
                            bot.sendDocument(
                                chatId = chatId,
                                document = TelegramFile.ByFile(it),
                            )
                            logger.info("[finish command] chat ${chatId.id} sending zip file...done")
                            cleanup(chatId)
                        }
                    } else {
                        bot.sendMessage(chatId, langPack.getString("oops"))
                    }
                }

            }
        }

    }

    private suspend fun CommandHandlerEnvironment.downloadHandler(
        fpath: Fpath,
        stickerCollectPack: StickerCollectPack,
        langPack: LanguagePack
    ) {
        val chatId = currentChatId()
        logger.info("[finish command] chat ${chatId.id} Downloading files...")
        coroutineScope {
            val deferredPaths = async {
                //获取文件路径
                resolveFile(stickerCollectPack.files, null, langPack)
            }
            launch {
                withContext(Dispatchers.IO) {
                    val paths = deferredPaths.await()
                    for (path in paths) {
                        val srcPath = fpath.srcPath
                        val destFile = "$srcPath${Paths.get(path).basename()}"
                        download(path, destFile)
                    }
                }
            }
        }
    }

    /**
     * 返回文件链接
     */
    private suspend fun CommandHandlerEnvironment.resolveFile(
        fileIds: Set<String>,
        inreplyTo: Long?,
        langPack: LanguagePack
    ): Array<String>/*file path url*/ {
        //不要在意这是胶水代码，谁让这一堆Environment都没有什么继承关系呢。
        val paths: Array<String> = Array(fileIds.size) { "" }
        var pathsOffset = 0
        val chatId = currentChatId()
        var fid = ""
        return withContext(Dispatchers.IO) {
            try {
                for (fileId in fileIds) {
                    fid = fileId
                    val (file, e) = bot.getFile(fileId)
                    if (e != null) throw e
                    val result = file!!.body()!!.result!!
                    val path = result.filePath!!
                    paths[pathsOffset++] = path
                }
                paths
            } catch (ignore: RuntimeException) {
                bot.sendMessage(chatId, langPack.getString("error.err_get_filelink"), replyToMessageId = inreplyTo)
                throw DownloadFileException("[finish command] get file link for $fid failed")
            }
        }
    }

    private suspend fun CommandHandlerEnvironment.download(url: String, dest: String) {
        val chatId = currentChatId()
        var newDest = dest
        try {
            //创建文件，用于下载
            val outputStream = withContext(Dispatchers.IO) {
                Files.newOutputStream(Path(newDest), StandardOpenOption.CREATE, StandardOpenOption.WRITE)
            }
            //开始下载文件
            val (res, e) = bot.downloadFile(url)
            if (e != null) throw e
            val responseBody = res!!.body()!!
            responseBody.use { input ->
                input.byteStream().use { it.copyTo(outputStream) }
            }
            //简单判断一下扩展名
            if (dest.indexOf('.') == -1) {
                newDest = "$dest.webp"
                withContext(Dispatchers.IO) {
                    Files.move(Path(dest), Path(newDest))
                    logger.info("[finish command] chat $chatId file $url saved to $newDest")
                }
            }
            //回写到redis
            logger.info("[finish command] chat $chatId download file $url saved to $newDest")
            val collectPack = redisService.getCurrentPack(chatId)!!
            val srcImg = collectPack.srcImg
            val list = if (srcImg.isEmpty()) {
                setOf(newDest)
            } else {
                srcImg.toMutableSet().apply { add(newDest) }
            }
            redisService.setCurrentPack(chatId, collectPack.copy(srcImg = list))

        } catch (e: RuntimeException) {
            //删除磁盘上的文件并记录日志
            withContext(Dispatchers.IO) {
                Files.deleteIfExists(Path(newDest))
                logger.error("[finish command] chat $chatId deleting file error $newDest", e)
            }
        }
    }

    private suspend fun CommandHandlerEnvironment.convertHandler(
        fpath: Fpath,
        format: String?,
        width: String?,
        langPack: LanguagePack,
    ) {
        val width = width?.toDouble()
        val chatId = currentChatId()
        logger.info("[finish command] chat ${chatId.id} Converting images...")
        val collectPack = redisService.getCurrentPack(chatId)
        val srcImg = collectPack!!.srcImg
        val destImg = ConcurrentLinkedQueue<String>()
        coroutineScope {
            withContext(Dispatchers.IO) {
                srcImg.forEach { srcPath ->
                    launch {
                        try {
                            //假设所有下载得到的文件都有文件后缀
                            val convert = convert(srcPath, fpath)
                            destImg.add(convert)
                            return@launch
                        } catch (ignore: RuntimeException) {
                            logger.error("[finish command] chat ${chatId.id} Converting images error", ignore)
                            bot.sendMessage(chatId, langPack.getString("sticker.no_support"))
                        }
                    }
                }
            }
        }
        val list = destImg.toSet()
        redisService.setCurrentPack(chatId, collectPack.copy(destImg = list))
        logger.info("[finish command] chat ${chatId.id} Converting images end")
    }

    //用于图像转换
//    private suspend fun CommandHandlerEnvironment.convert(
//        srcPath: String,
//        fpath: Fpath,
//        format: String,
//        width: Double
//    ): String {
//        val chatId = currentChatId()
//        val imgpath = fpath.imgPath
//        val fileName = Path(srcPath).fileName.name.let {
//            if (it.lastIndexOf('.') != -1) {
//                it.substring(0, it.lastIndexOf('.') + 1)
//            } else {
//                it
//            }
//        }
//        //构建新文件路径和文件扩展名
//        val newImgPath = "$imgpath$fileName$format"
//        withContext(Dispatchers.IO) {
//            OpenCVService.conversionImageFile(srcPath.drop(2), newImgPath.drop(2), width, width, 1.0)
//        }
//        logger.info("[finish command] chat ${chatId.id} ConversionImage save to $newImgPath")
//        return newImgPath
//    }
    private suspend fun convert(srcPath: String, fpath: Fpath): String {
        if (srcPath.lastIndexOf('.') == -1) {
            val suffix = srcPath.suffix()
            val srcPath = Path(srcPath)
            val destPath = fpath.imgPath + srcPath.basename()
            val savePath = when (suffix) {
                "webp" -> {
                    copyFile(srcPath, Path(destPath))
                    destPath
                }

                "webm" -> {
                    val abstractFile = destPath.removeSuffix("webm") + "gif"
                    val dest = Path(abstractFile)
                    OpenCVService.videoToGif(srcPath, dest)
                    abstractFile
                }

                else -> throw RuntimeException()
            }
            return savePath
        }
        throw RuntimeException()
    }

    //将文件压缩为zip
    private suspend fun CommandHandlerEnvironment.zipHandler(): String {
        val chatId = currentChatId()
        logger.info("[finish command] chat ${chatId.id} Adding files to zip file...")
        //待压缩
        val zipPath = "${Path("").toAbsolutePath().pathString}${GlobalResource.imageStorage.drop(1)}/${chatId.id}"
        val fpath = Fpath(zipPath)
        val collectPack = redisService.getCurrentPack(chatId)!!
        return withContext(Dispatchers.IO) {
            val zipEntryPath = "${fpath.packPath}/${chatId.id}.zip"
            collectPack.destImg.let { destimgs ->
                if (destimgs.isEmpty()) throw RuntimeException()
                val files = destimgs.map { dest ->
                    Path(dest).toFile()
                }
                val zipOutputStream =
                    ZipOutputStream(FileOutputStream(Path(zipEntryPath).toFile()))
                zipOutputStream.use {
                    it.setComment(zipCommand)
                    it.setLevel(5)
                    files.forEach { file ->
                        try {
                            file.inputStream().use { input ->
                                zipOutputStream.putNextEntry(ZipEntry(file.name))
                                logger.info("[finish command] chat ${chatId.id} Adding file ${file.name} to zip file...")
                                input.copyTo(zipOutputStream)
                                zipOutputStream.closeEntry()
                            }
                        } catch (ignore: Exception) {

                        }
                    }
                    it.finish()
                }
            }
            zipEntryPath
        }
    }

    @OptIn(ExperimentalPathApi::class)
    private suspend fun CommandHandlerEnvironment.cleanup(chat: ChatId.Id) {
        logger.info("[finish command] chat ${chat.id} file Cleanup")
        withContext(Dispatchers.IO) {
            redisService.removeCurrentPack(chat)
            val path = Path("${currentPath()}${GlobalResource.imageStorage.drop(1)}/${chat.id}")
            path.deleteRecursively()
        }
    }

    override val dispatcherName: String = "为用户创建一个贴纸收集包"
    override val description: String = "newStickerCollect"
}

class DownloadFileException(msg: String, cause: Throwable? = null) : RuntimeException(msg, cause)

suspend fun RedisService.getCurrentPack(id: ChatId.Id): StickerCollectPack? {
    return get("${RedisKeys.newPack}:${id.id}")
}

suspend fun RedisService.setCurrentPack(id: ChatId.Id, pack: StickerCollectPack) {
    set("${RedisKeys.newPack}:${id.id}", pack)
}

suspend fun RedisService.removeCurrentPack(id: ChatId.Id) {
    remove("${RedisKeys.newPack}:${id.id}")
}