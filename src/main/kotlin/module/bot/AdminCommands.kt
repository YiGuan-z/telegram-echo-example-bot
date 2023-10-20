package module.bot

import application.BotDSL
import application.BotDispatcher
import application.createBotDispatcherModule
import com.github.kotlintelegrambot.dispatcher.Dispatcher

/**
 *
 * @author caseycheng
 * @date 2023/10/15-14:16
 * @doc 这里存放所有管理员的命令
 **/
val adminCommands = createBotDispatcherModule("adminCommands", ::AdminCommandsConfiguration) {
    AdminCommands()
}

@BotDSL
class AdminCommandsConfiguration {
    var adminUser: String? = null
}

@BotDSL
fun AdminCommandsConfiguration.setAdminUsername(username: String) {
    this.adminUser = username
}


class AdminCommands : BotDispatcher {
    // start server
    // stop server
    override val dispatch: Dispatcher.() -> Unit
        get() = TODO("Not yet implemented")
    override val dispatcherName: String
        get() = TODO("Not yet implemented")
    override val description: String
        get() = TODO("Not yet implemented")
}