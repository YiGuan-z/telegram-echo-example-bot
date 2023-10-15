package module.bot

import application.*
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.command
import module.bot.modal.ChatLangProfile
import module.currentChatId
import module.redis.RedisService
import module.thisLogger
import org.slf4j.Logger

/**
 *
 * @author caseycheng
 * @date 2023/10/15-00:18
 * @doc
 **/
class LangCommand(
    private val i18nPacks: I18nPacks,
    private val redisService: RedisService,
    private val langCommand: String
) : BotDispatcher {
    private val logger: Logger = thisLogger<LangCommand>()
    override val dispatch: Dispatcher.() -> Unit = {
        command(langCommand) {
            val chatId = currentChatId()
            val redisPath = "${RedisKeys.userChatKeys}:${chatId.id}"
            //由于之前有一个初始化用户的处理器，所以这里基本上不会为空
            val profile = redisService.get<ChatLangProfile>(redisPath) ?: return@command
            val languagePack = i18nPacks.get(profile.lang)
            try {
                message.text?.let {
                    if (it.length == langCommand.length + 1) {
                        val replyMsg =
                            languagePack.getString("lang.lang_select_msg", "list", buildString {
                                append('\n')
                                i18nPacks.keys().forEach { lang ->
                                    append('[')
                                    append(lang)
                                    append(']')
                                    append('\n')
                                }
                            })
                        bot.sendMessage(chatId, replyMsg)
                    } else {
                        val chooseLang = it.drop(langCommand.length + 1).trim()
                        bot.sendMessage(chatId, languagePack.getString("lang.lang_choose_prompt", "lang", chooseLang))
                        redisService.set(redisPath, profile.copy(lang = Language.of(chooseLang)))
                        bot.sendMessage(chatId, languagePack.getString("lang.lang_changed", "lang", chooseLang))
                    }
                }
            } catch (e: Exception) {
                logger.error("[LangCommand] 会话 $chatId 的语言切换处理器出现错误，详情请检查日志信息", e)
                bot.sendMessage(currentChatId(), languagePack.getString("lang.lang_command_user_error"))
            }

        }
    }
    override val dispatcherName: String = "langCommand"
    override val description: String = "/lang 后接语言标识，即可对语言进行切换"
}

class LangConfiguration {
    var i18nPack: I18nPacks? = null
    var redisService: RedisService? = null
    var languageChangeCommand: String = "lang"
}

@BotDSL
fun LangConfiguration.setRedisService(redisService: RedisService) {
    this.redisService = redisService
}

@BotDSL
fun LangConfiguration.setI18n(i18n: I18nPacks) {
    this.i18nPack = i18n
}

val langCommand = createBotDispatcherModule("langCommand", ::LangConfiguration) { config ->
    val redisService = requireNotNull(config.redisService) { "need redisService instance" }
    val i18n = requireNotNull(config.i18nPack) { "need I18nPacks interface instance" }
    val langCommand = config.languageChangeCommand
    LangCommand(i18n, redisService, langCommand)
}