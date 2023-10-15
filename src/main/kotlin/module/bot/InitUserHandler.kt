package module.bot

import application.BotDispatcher
import application.createBotDispatcherModule
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.text
import module.bot.modal.ChatLangProfile
import module.currentChatId
import module.redis.RedisService

/**
 *
 * @author caseycheng
 * @date 2023/10/14-14:25
 * @doc
 **/
class InitUserHandler(val redisService: RedisService, val defaultLang: String) : BotDispatcher {
    override val dispatch: Dispatcher.() -> Unit = {
        text {
            val chatId = currentChatId()
            val redisPath = "${RedisKeys.userChatKeys}:${chatId.id}"
            val langProfile = redisService.get<ChatLangProfile>(redisPath)
            if (langProfile == null) {
                redisService.set(redisPath, ChatLangProfile(chatId.id, defaultLang))
            }
        }
    }
    override val dispatcherName: String = "init user lang"
    override val description: String = "each users default interface lang"
}

class InitUserConfiguration : ChatHandlerConfiguration() {
    //设置默认语言
    lateinit var defaultLang: String
}

fun InitUserConfiguration.setDefaultLang(lang: String) {
    this.defaultLang = lang
}

val initUserHandler = createBotDispatcherModule("initUser", ::InitUserConfiguration) {
    InitUserHandler(it.redisService, it.defaultLang)
}