package github.cheng.module.bot

import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.handlers.MessageHandlerEnvironment
import com.github.kotlintelegrambot.dispatcher.message
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.MessageEntity
import com.github.kotlintelegrambot.entities.TelegramFile
import com.github.kotlintelegrambot.entities.stickers.Sticker
import com.github.kotlintelegrambot.entities.stickers.StickerSet
import github.cheng.TelegramResources
import github.cheng.application.*
import github.cheng.module.bot.modal.Fpath
import github.cheng.module.bot.modal.StickerCollectPack
import github.cheng.module.currentChatId
import github.cheng.module.opencv.OpenCVService
import github.cheng.module.redis.RedisService
import github.cheng.module.thisLogger
import kotlinx.coroutines.*
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.deleteRecursively
import kotlin.io.path.name

/**
 *
 * @author caseycheng
 * @date 2023/10/17-00:36
 * @doc 对消息进行处理
 **/
@JvmField
val messageHandler =
    createBotDispatcherModule("messageHandler", ::MessageHandlerConfiguration) { config ->
        val redisService = requireNotNull(config.redisService) { "redisService is null" }
        val i18nPacks = requireNotNull(config.i18nPacks) { "i18nPacks is null" }
        MessageHandler(redisService, i18nPacks)
    }

class MessageHandler(
    @Suppress("MemberVisibilityCanBePrivate")
    @JvmField
    val redisService: RedisService,
    @Suppress("MemberVisibilityCanBePrivate")
    @JvmField
    val i18nPacks: I18nPacks
) : BotDispatcher {
    @JvmField
    val logger = thisLogger<MessageHandler>()

    override val dispatch: Dispatcher.() -> Unit = {
        message {
            if (message.chat.type != "private") return@message
            val chatId = currentChatId()
            logger.info("receive message from ${message.chat.id}")
            // 前面有一层[InitUserHandler]在创建语言配置，所以这里是不会为空的。
            val langProfile = redisService.currentChatLangProfile(chatId)!!
            val languagePack = i18nPacks.get(langProfile.lang)
            try {
                val currentPack = redisService.getCurrentPack(chatId)
                when {
                    currentPack != null && !currentPack.isLocked -> {
                        val sticker = message.sticker
                        if (sticker != null) {
                            addSticker(languagePack, sticker)
                        }
                        message.entities?.forEach { messageEntity ->
                            if (messageEntity.type == MessageEntity.Type.URL) {
                                val url =
                                    message.text!!.slice(messageEntity.offset..<messageEntity.offset + messageEntity.length)
                                val legalSource =
                                    run {
                                        val find = TelegramResources.stickerSources.find { url.startsWith(it) }
                                        find != null
                                    }
                                if (legalSource && url.length > 25) {
                                    stickerSetHandler(languagePack, Path(url).fileName.name)
                                } else {
                                    bot.sendMessage(
                                        chatId,
                                        languagePack.getString("sticker.unsupported_sticker_source", "source", url),
                                    )
                                }
                            }
                        }
                    }

                    currentPack == null && message.sticker != null -> directHandler(languagePack)

                    currentPack == null -> {
                        bot.sendMessage(chatId, languagePack.getString("start"))
                    }

                    else -> {
                        val isStart = currentPack.isLocked
                        if (isStart) {
                            bot.sendMessage(chatId, languagePack.getString("start"))
                        } else {
                            bot.sendMessage(chatId, languagePack.getString("newpack.tasklocked"))
                        }
                    }
                }
            } catch (e: RuntimeException) {
                bot.sendMessage(chatId, languagePack.getString("error.user_prompt"))
            }
        }
    }

    private suspend fun MessageHandlerEnvironment.addSticker(
        languagePack: LanguagePack,
        sticker: Sticker,
    ) {
        val chatId = currentChatId()
        val currentPack = redisService.getCurrentPack(chatId)!!
        if (currentPack.files.contains(message.sticker?.fileId)) {
            bot.sendMessage(
                chatId,
                languagePack.getString("sticker.duplicated_sticker"),
                replyToMessageId = message.messageId,
            )
            return
        }

        if (currentPack.files.size >= TelegramResources.maxImages) {
            bot.sendMessage(chatId, languagePack.getString("sticker.taskfull"))
            return
        }

        val files = currentPack.files.toMutableSet().apply { add(sticker.fileId) }
        redisService.setCurrentPack(chatId, currentPack.copy(files = files))
        val remain = TelegramResources.maxImages - files.size
        val message =
            if (remain != 0) {
                languagePack.getString("sticker.saved", "remain", remain.toString())
            } else {
                languagePack.getString("sticker.taskfull")
            }
        bot.sendMessage(chatId, message)
    }

    private suspend fun MessageHandlerEnvironment.stickerSetHandler(
        languagePack: LanguagePack,
        setName: String,
    ) {
        val chatId = currentChatId()
        val currentPack = redisService.getCurrentPack(chatId)!!
        bot.sendMessage(chatId, languagePack.getString("sticker.get_set_info"))
        val (result, e) = bot.getStickerSet(setName)
        if (e != null) throw e
        try {
            val set = result!!.body()!!.result!!
            if (currentPack.files.size + set.stickers.size >= TelegramResources.maxImages) {
                bot.sendMessage(
                    chatId,
                    languagePack.getString(
                        "newpack.taskexceed",
                        "count" to currentPack.files.size.toString(),
                        "max" to TelegramResources.maxImages.toString(),
                    ),
                )
                return
            }
            logger.info("[Message Handler] chat ${chatId.id} get Sticker Set $setName")
            val newPack = addSet(currentPack, set)
            redisService.setCurrentPack(chatId, newPack)
            val originCount = newPack.files.size
            bot.sendMessage(
                chatId,
                languagePack.getString(
                    "sticker.set_added_count",
                    "sticker_count" to originCount.toString(),
                    "count" to (TelegramResources.maxImages - originCount).toString(),
                ),
            )
            return
        } catch (e: RuntimeException) {
            logger.error("[Message Handler] get Sticker $setName failed", e)
            bot.sendMessage(chatId, languagePack.getString("sticker.invalid_set", "setName", setName))
            return
        }
    }

    private fun addSet(
        collectPack: StickerCollectPack,
        set: StickerSet,
    ): StickerCollectPack {
        val files = collectPack.files.toMutableSet()
        set.stickers.forEach { sticker ->
            if (!files.contains(sticker.fileId)) {
                files.add(sticker.fileId)
            }
        }
        return collectPack.copy(files = files)
    }

    private suspend fun MessageHandlerEnvironment.directHandler(languagePack: LanguagePack) {
        val chatId = currentChatId()
        val messageId = message.messageId
        val collectPack = newPackHandler()
        try {
            redisService.setCurrentPack(chatId, collectPack.copy(isLocked = true))
            val packpath = "${TelegramResources.imageStorage}/${chatId.id}"
            val fpath = Fpath(packpath)
            coroutineScope {
                withContext(Dispatchers.IO) {
                    fpath.mkdirFinder()
                    logger.info("[Message Handler] chat ${chatId.id} started direct image task.")
                    val pendingMsg =
                        bot.sendMessage(chatId, languagePack.getString("sticker.direct_task_started")).get()
                    val fileUrl = resolveFile(message.sticker!!.fileId, messageId, languagePack)
                    val job0 =
                        async(Dispatchers.IO) {
                            try {
                                download(fileUrl, fpath.srcPath + Path(fileUrl).basename())
                            } catch (e: Exception) {
                                logger.error("[finish command] download file failed: $fileUrl")
                                bot.sendMessage(currentChatId(), languagePack.getString("error.download_error"))
                                throw e
                            }
                        }
                    // 转换
                    // 俺寻思 webp可以不用转换
                    val job1 =
                        async(Dispatchers.IO) {
                            val destPath = job0.await()
                            try {
                                if (destPath.lastIndexOf('.') != -1) {
                                    val suffix = destPath.suffix()
                                    val srcPath = Path(destPath)
                                    val destPath = fpath.imgPath + srcPath.basename()
                                    val file =
                                        when (suffix) {
                                            "webp" -> {
                                                copyFile(srcPath, Path(destPath))
                                                destPath
                                            }

                                            "webm" -> {
                                                val destPath = destPath.removeSuffix("webm") + "gif"
                                                val dest = Path(destPath)
                                                OpenCVService.videoToGif(srcPath, dest)
                                                destPath
                                            }

                                            else -> throw RuntimeException()
                                        }
                                    return@async file
                                }
                                throw RuntimeException()
                            } catch (e: Exception) {
                                logger.error("[Message Handler] chat ${chatId.id} convert error")
                                bot.sendMessage(chatId, languagePack.getString("error.convert_error"))
                                throw e
                            }
                        }
                    launch(Dispatchers.IO) {
                        val photo = job1.await()
                        bot.sendDocument(
                            chatId,
                            document = TelegramFile.ByFile(Path(photo).toFile()),
                            replyToMessageId = messageId,
                        )
                        bot.deleteMessage(chatId, pendingMsg.messageId)
                        cleanup(chatId)
                    }
                }
            }
        } catch (e: RuntimeException) {
            logger.info("[Message Handler] chat ${chatId.id} failed direct image task. cleaning up...")
            cleanup(chatId)
        }
    }

    private suspend fun MessageHandlerEnvironment.newPackHandler(): StickerCollectPack {
        val chatId = currentChatId()
        logger.info("[MessageHandle] chat ${chatId.id} Started a new pack task")
        val stickerCollectPack = StickerCollectPack(start = message.date)
        redisService.setCurrentPack(chatId, StickerCollectPack(start = message.date))
        return stickerCollectPack
    }

    private suspend fun MessageHandlerEnvironment.resolveFile(
        fileIds: String,
        inreplyTo: Long?,
        langPack: LanguagePack,
    ): String/*file path url*/ {
        // 不要在意这是胶水代码，谁让这一堆Environment都没有什么继承关系呢。
        val chatId = currentChatId()
        return withContext(Dispatchers.IO) {
            try {
                val (result, e) = bot.getFile(fileIds)
                if (e != null) throw e
                result!!.body()!!.result!!.filePath!!
            } catch (ignore: RuntimeException) {
                bot.sendMessage(chatId, langPack.getString("error.err_get_filelink"), replyToMessageId = inreplyTo)
                throw DownloadFileException("[finish command] get file link for $fileIds failed")
            }
        }
    }

    private suspend fun MessageHandlerEnvironment.download(
        url: String,
        destPath: String,
    ): String {
        try {
            withContext(Dispatchers.IO) {
                val outputStream =
                    Files.newOutputStream(Path(destPath), StandardOpenOption.CREATE, StandardOpenOption.WRITE)
                val (res, e) = bot.downloadFile(url)
                if (e != null) throw e
                val responseBody = res!!.body()!!
                responseBody.use { input ->
                    input.byteStream().use { it.copyTo(outputStream) }
                }
            }
            return destPath
        } catch (e: Exception) {
            throw e
        }
    }

    private suspend fun MessageHandlerEnvironment.videoToGif(
        srcPath: String,
        fpath: Fpath,
        extension: String,
    ): String {
        val chatId = currentChatId()
        val imgpath = fpath.imgPath
        val fileName =
            Path(srcPath).fileName.name.let {
                if (it.lastIndexOf('.') != -1) {
                    it.substring(0, it.lastIndexOf('.'))
                } else {
                    it
                }
            }
        // 构建新文件路径和文件扩展名
        val newImgPath = "$imgpath$fileName.$extension"
        withContext(Dispatchers.IO) {
            OpenCVService.videoToGif(Path(srcPath), Path(newImgPath))
        }
        logger.info("[finish command] chat ${chatId.id} ConversionImage save to $newImgPath")
        return newImgPath
    }

    @OptIn(ExperimentalPathApi::class)
    private suspend fun cleanup(chat: ChatId.Id) {
        logger.info("[Message Handle] chat ${chat.id} file Cleanup")
        withContext(Dispatchers.IO) {
            redisService.removeCurrentPack(chat)
            val path = Path("${currentPath()}${TelegramResources.imageStorage.drop(1)}/${chat.id}")
            path.deleteRecursively()
        }
    }

    override val dispatcherName: String = "messageHandler"
    override val description: String = "用于处理贴纸消息"
}

class MessageHandlerConfiguration {
    var redisService: RedisService? = null
    var i18nPacks: I18nPacks? = null
}

@BotDSL
fun MessageHandlerConfiguration.setRedisService(redisService: RedisService) {
    this.redisService = redisService
}

@BotDSL
fun MessageHandlerConfiguration.setI18nPacks(i18nPacks: I18nPacks) {
    this.i18nPacks = i18nPacks
}
