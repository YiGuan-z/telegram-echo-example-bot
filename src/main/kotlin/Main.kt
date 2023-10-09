import application.Application
import module.echoChatHandler
import module.jackson
import module.redis.RedisService
import module.redis.jacksonRedisCodec
import module.redis.redisFactory

fun main(args: Array<String>) = Application.main(args, Application::configModule)

fun Application.configModule() {
    install(jacksonRedisCodec) {
        mapper = install(jackson)
    }
    install(redisFactory) {
        url = env("TG_BOT_REDIS_URL")
    }
    install(echoChatHandler) {
        redisService = RedisService()
    }
}


