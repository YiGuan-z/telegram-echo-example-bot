package module

import com.github.kotlintelegrambot.dispatcher.handlers.TextHandlerEnvironment
import com.github.kotlintelegrambot.entities.ChatId

/**
 *
 * @author caseycheng
 * @date 2023/10/8-01:28
 * @doc
 **/
fun TextHandlerEnvironment.currentChatId() = ChatId.fromId(message.chat.id)