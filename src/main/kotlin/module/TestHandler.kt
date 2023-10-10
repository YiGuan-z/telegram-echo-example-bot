package module

import application.BotDispatcher
import application.createBotDispatcherModule
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.text

/**
 *
 * @author caseycheng
 * @date 2023/10/10-11:54
 * @doc
 **/
class TestHandler : BotDispatcher {

    override val dispatch: Dispatcher.() -> Unit = {
        text {
            bot.sendMessage(currentChatId(), text)
        }
    }

    override val dispatcherName: String = "echo1"
    override val description: String = "二号复读机"
}

class HandlerConfig {

}

val testHandler = createBotDispatcherModule("textHandler", ::HandlerConfig) { config ->
    TestHandler()
}