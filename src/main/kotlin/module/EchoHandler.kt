package module

import application.BotDispatcher
import application.createBotDispatcherModule
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.handlers.media.MediaHandlerEnvironment
import com.github.kotlintelegrambot.dispatcher.sticker
import com.github.kotlintelegrambot.dispatcher.text
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.stickers.Sticker
import module.redis.RedisService
import setOnce

/**
 *
 * @author caseycheng
 * @date 2023/10/2-18:34
 * @doc 复读机模式
 **/
class EchoChatHandler(config: EchoChatHandlerConfiguration) :
    BotDispatcher {
    val redisService = config.redisService
    override fun Dispatcher.dispatch() {
        text("echo") {
            //或许可以抽象出一个责任链
            //或者在全局做路由，反正这个带text参数的方法挺抽象的。
            val chatId = currentChatId()
            bot.sendMessage(chatId, "你好，我可以当一个复读机")
        }
        text {
            val currentChat = ChatId.fromId(message.chat.id)
            bot.sendMessage(currentChat, text)
        }
        sticker {
            val currentChat = ChatId.fromId(message.chat.id)
            bot.sendSticker(currentChat, remoteSticker(), replyMarkup = message.replyMarkup)
        }
    }
}

fun MediaHandlerEnvironment<Sticker>.remoteSticker() = this.message.sticker?.fileId ?: ""

class EchoChatHandlerConfiguration {
    var redisService: RedisService<Any, Any> by setOnce()
}

val echoChatHandler = createBotDispatcherModule(
    "echoChatHandler",
    ::EchoChatHandlerConfiguration
) { config ->
    EchoChatHandler(config)
}
