package module.bot

import GlobalResource
import application.*
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.handlers.CommandHandlerEnvironment
import com.github.kotlintelegrambot.entities.ChatId
import kotlinx.coroutines.*
import module.bot.modal.ChatLangProfile
import module.currentChatId
import module.redis.RedisService
import module.redis.currentChatLangProfile
import module.thisLogger
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.io.path.Path
import kotlin.io.path.createDirectories

/**
 *
 * @author caseycheng
 * @date 2023/10/15-15:27
 * @doc 可以依赖[ChatLangProfile]对象，但是不能在内部加字段，在redis中开辟一片新内存也是可以的。
 **/
val newPackCommand = createBotDispatcherModule("newPackCommand", ::NewPackCommandConfiguration) { config ->
    val redisService = requireNotNull(config.redisService) { "need redisService" }
    val i18n = requireNotNull(config.i18nPacks) { "need i18nPacks" }
    NewPackCommand(redisService, i18n)
}

class NewPackCommandConfiguration {
    var redisService: RedisService? = null
    var i18nPacks: I18nPacks? = null
}

@BotDSL
fun NewPackCommandConfiguration.setRedisService(redisService: RedisService) {
    this.redisService = redisService
}

@BotDSL
fun NewPackCommandConfiguration.setI18n(i18nPacks: I18nPacks) {
    this.i18nPacks = i18nPacks
}

class NewPackCommand(private val redisService: RedisService, private val i18n: I18nPacks) : BotDispatcher {
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
                val stickerCollectPack = redisService.getCurrentPack(chatId)
                stickerCollectPack?.let {
                    if (it.isLocked) {
                        bot.sendMessage(chatId, languagePack.getString("newpack.tasklocked"))
                        return@command
                    }
                    val map = args.resolveArgs()
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
                bot.sendMessage(chatId, languagePack.getString("error.user_prompt"))
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
        val fpath = mutableMapOf(
            "packpath" to packPath,
            "srcpath" to "${packPath}/src/",
            "imgpath" to "${packPath}/img/",
        )

        Paths.get(fpath["packpath"]!!).toAbsolutePath().createDirectories()
        Paths.get(fpath["srcpath"]!!).toAbsolutePath().createDirectories()
        Paths.get(fpath["imgpath"]!!).toAbsolutePath().createDirectories()

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
                    convertHandler(fpath, format, width)
                }
                val job3 = async {
                    job2.await()
                    //📦打包贴纸
                    bot.sendMessage(chatId, langPack.getString("downloadstep.packaging"))
                }
                job3.await()
            }
        }
        //发送贴纸
        bot.sendMessage(chatId, langPack.getString("msg.send"))


    }

    private suspend fun CommandHandlerEnvironment.downloadHandler(
        fpath: Map<String, String>,
        stickerCollectPack: StickerCollectPack,
        langPack: LanguagePack
    ) {
        val chatId = currentChatId()
        logger.info("[finish command] Downloading files...")
        coroutineScope {
            val deferredPaths = async {
                //获取文件路径
                resolveFile(stickerCollectPack.files, null, langPack)
            }
            launch {
                withContext(Dispatchers.IO) {
                    val paths = deferredPaths.await()
                    for (path in paths) {
                        val srcPath = fpath["srcpath"]!!
                        val destFile = "$srcPath${Paths.get(path)}"
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
        fileIds: List<String>,
        inreplyTo: Long?,
        langPack: LanguagePack
    ): Array<String>/*file path url*/ {
        val paths: Array<String> = Array(fileIds.size) { "" }
        var pathsOffset = 0
        val chatId = currentChatId()
        var fid = ""
        try {
            for (fileId in fileIds) {
                fid = fileId
                val (file, e) = bot.getFile(fileId)
                if (e != null) throw e
                val result = file!!.body()!!.result!!
                val path = result.filePath!!
                paths[pathsOffset++] = path
            }
            return paths
        } catch (ignore: RuntimeException) {
            bot.sendMessage(chatId, langPack.getString("error.err_get_filelink"), replyToMessageId = inreplyTo)
            throw DownloadFileException("[finish command] get file link for $fid failed")
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
                listOf(newDest)
            } else {
                srcImg.toMutableList().apply { add(newDest) }
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
        fpath: Map<String, String>,
        format: String?,
        width: String?,
    ) {
        TODO()
    }

    override val dispatcherName: String = "为用户创建一个贴纸收集包"
    override val description: String = "newStickerCollect"
}

class DownloadFileException(msg: String, cause: Throwable? = null) : RuntimeException(msg, cause)

private suspend fun RedisService.getCurrentPack(id: ChatId.Id): StickerCollectPack? {
    return get("${RedisKeys.newPack}:${id.id}")
}

private suspend fun RedisService.setCurrentPack(id: ChatId.Id, pack: StickerCollectPack) {
    set("${RedisKeys.newPack}:${id.id}", pack)
}

//先这样吧，js里面看不出这些是个什么类型，一步一步慢慢来。
data class StickerCollectPack @JvmOverloads constructor(
    val start: Long = -1,
    val files: List<String> = emptyList(),
    val srcImg: List<String> = emptyList(),
    val destImg: List<String> = emptyList(),
    val isLocked: Boolean = false,
)

fun StickerCollectPack.startTime(): LocalDateTime? {
    return if (start == -1L) {
        null
    } else {
        LocalDateTime
            .ofInstant(Instant.ofEpochSecond(start), ZoneOffset.ofHours(8))
    }
}