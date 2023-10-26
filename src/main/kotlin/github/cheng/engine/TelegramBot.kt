package github.cheng.engine

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

object TelegramBot : ApplicationEngine {
    override fun create(application: Application): Application {
        with(application) {
            configGlobalResource()
            mkdirImageFinder()
            configBot()
        }
        return application
    }

    override fun Application.start() {
        instance(bot).startPolling().also { thisLogger<Application>().info("机器人已启动") }
    }

    override fun Application.stop() {
        instance(bot).stopPolling()
    }
}

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

private fun Application.configBot() {
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