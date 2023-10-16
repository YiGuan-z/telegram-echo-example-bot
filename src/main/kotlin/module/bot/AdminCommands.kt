package module.bot

import application.BotDispatcher
import application.createBotDispatcherModule
import com.github.kotlintelegrambot.dispatcher.Dispatcher

/**
 *
 * @author caseycheng
 * @date 2023/10/15-14:16
 * @doc 这里存放所有管理员的命令
 **/
val adminCommands = createBotDispatcherModule("adminCommands",::AdminCommandsConfiguration){
    AdminCommands()
}
class AdminCommandsConfiguration{

}
class AdminCommands:BotDispatcher {
    override val dispatch: Dispatcher.() -> Unit
        get() = TODO("Not yet implemented")
    override val dispatcherName: String
        get() = TODO("Not yet implemented")
    override val description: String
        get() = TODO("Not yet implemented")
}