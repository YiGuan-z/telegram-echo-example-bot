package github.cheng

import github.cheng.application.Application
import github.cheng.application.getJsonLanguageFiles
import github.cheng.application.i18n
import github.cheng.engine.TelegramBot
import github.cheng.module.bot.*
import github.cheng.module.ignoreUnknownProperties
import github.cheng.module.jackson
import github.cheng.module.redis.RedisFactory
import github.cheng.module.redis.RedisService
import github.cheng.module.redis.jacksonRedisCodec
import github.cheng.module.redis.redisFactory
import kotlinx.coroutines.delay

fun main(args: Array<String>) = Application.main(args, TelegramBot, Application::configModule)

fun Application.configModule() {
    install(jackson) {
        // 忽略未知属性
        ignoreUnknownProperties(true)
    }
    install(i18n) {
        generateLanguages = { getJsonLanguageFiles() }
    }
    configurationRedis()
    configurationBotModule()
}

fun Application.configurationRedis() {
    install(jacksonRedisCodec) {
        mapper = instance(jackson)
    }

    install(redisFactory) {
        url = appEnvironment.property("bot.redis_url").getString()
    }
}

fun Application.configurationBotModule() {
    val redisAsyncClient = RedisFactory.newAsyncClient(instance(jacksonRedisCodec))
    val redisService = RedisService(redisAsyncClient, instance(jackson))

    install(initUserHandler) {
        setRedisService(redisService)
        setDefaultLang(appEnvironment.property("bot.lang.default").getString())
    }

    install(langCommand) {
        setRedisService(redisService)
        setI18n(instance(i18n))
        languageChangeCommand = "lang"
    }

    install(newPackCommand) {
        setI18n(instance(i18n))
        setRedisService(redisService)
        zipCommand = appEnvironment.property("bot.zip.comment").getString()
    }

    install(messageHandler) {
        setI18nPacks(instance(i18n))
        setRedisService(redisService)
    }
}
