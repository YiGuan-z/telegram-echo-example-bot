import application.Application
import application.getJsonFiles
import application.i18n
import module.bot.*
import module.ignoreUnknownProperties
import module.jackson
import module.opencv.OpenCVService
import module.redis.RedisFactory
import module.redis.RedisService
import module.redis.jacksonRedisCodec
import module.redis.redisFactory

suspend fun main(args: Array<String>) = Application.main(args, Application::configModule)

suspend fun Application.configModule() {
    OpenCVService.init()
    install(jackson) {
        //忽略未知属性
        ignoreUnknownProperties(true)
    }
    install(i18n) {
        generateLanguages = { getJsonFiles() }
    }
    configurationRedis()
    configurationBotModule()
}

fun Application.configurationRedis() {
    install(jacksonRedisCodec) {
        mapper = instance(jackson)
    }

    install(redisFactory) {
        url = appEnvironment.config("bot").property("redis_url").getString()
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



