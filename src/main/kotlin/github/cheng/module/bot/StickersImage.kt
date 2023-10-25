package github.cheng.module.bot

import github.cheng.application.BotDispatcher
import com.github.kotlintelegrambot.dispatcher.Dispatcher

/**
 *
 * @author caseycheng
 * @date 2023/10/2-14:02
 * @doc
 **/
class StickersImageHandler(override val dispatcherName: String, override val description: String) : BotDispatcher {
    override val dispatch: Dispatcher.() -> Unit
        get() = TODO("Not yet implemented")
}

