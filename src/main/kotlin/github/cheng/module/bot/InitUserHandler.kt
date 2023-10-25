package github.cheng.module.bot

import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.text
import github.cheng.application.BotDispatcher
import github.cheng.application.Language
import github.cheng.application.createBotDispatcherModule
import github.cheng.module.bot.modal.ChatLangProfile
import github.cheng.module.currentChatId
import github.cheng.module.redis.RedisService
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
                redisService.setChatLangProfile(chatId, ChatLangProfile(chatId.id, Language.of(defaultLang)))
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