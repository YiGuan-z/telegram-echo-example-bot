package github.cheng.engine

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.logging.LogLevel
import github.cheng.TelegramResources
import github.cheng.application.Application
import github.cheng.application.ApplicationEngine
import github.cheng.application.env.getListOrNull
import github.cheng.application.env.getStringOrNull
import github.cheng.module.bot.bot
import github.cheng.module.mkdirImageFinder
import github.cheng.module.thisLogger
import github.cheng.setOnce

object TelegramBot : ApplicationEngine {
    @Suppress("MemberVisibilityCanBePrivate")
    internal var botInstance: Bot by setOnce()
    @PublishedApi
    internal var application:Application by setOnce()

    override fun create(application: Application) {
        this.application = application
        with(application) {
            configGlobalResource()
            mkdirImageFinder()
            configBot()
        }
        botInstance = application.instance(bot)
    }

    override fun start() {
        botInstance.startPolling().also {
            val logger = thisLogger<Application>()
            val botAccount = botInstance.getMe().getOrNull()
                ?: throw TelegramBotTokenError("get bot account failed, please check bot token or network")
            botAccount.toString()
            logger.info("机器人已启动")
            logger.info("机器人账号信息: $botAccount")
        }
    }

    override fun stop() {
        botInstance.stopPolling()
    }
}

class TelegramBotTokenError(msg: String, cause: Throwable? = null) : RuntimeException(msg, cause)

context (Application)
private fun configGlobalResource() {
    appEnvironment.property("bot.lang.default").getStringOrNull()?.let {
        TelegramResources.defaultLang = it
    }
    appEnvironment.property("bot.master").getStringOrNull()?.let {
        TelegramResources.adminName = it
    }
    appEnvironment.property("bot.images.file_storage").getStringOrNull()?.let {
        TelegramResources.imageStorage = it
    }
    appEnvironment.property("bot.images.max_images").getStringOrNull()?.let {
        TelegramResources.maxImages = it.toInt()
    }
    appEnvironment.property("bot.images.sticker_sources").getListOrNull()?.let {
        TelegramResources.stickerSources = it
    }
}

context (Application)
private fun configBot() {
    install(bot) {
        token = appEnvironment.config("bot").property("tg_token").getString()
        logLevel = LogLevel.Error
        dispatch {
            val dispatcher = this
            botDispatcherModules.forEach { (_, botDispatcherModule) ->
                botDispatcherModule.apply { dispatcher.dispatch() }
                logger.info("Bot Dispatcher Module loaded: {}", botDispatcherModule.dispatcherName)
                logger.info("Bot Dispatcher Module description: {}", botDispatcherModule.description)
            }
        }
    }
}
