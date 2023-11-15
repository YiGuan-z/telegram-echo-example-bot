package github.cheng.module.bot

import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.handlers.CommandHandlerEnvironment
import github.cheng.application.BotDispatcher
import github.cheng.application.createBotDispatcherModule
import github.cheng.application.env.commandArgs
import github.cheng.module.redis.RedisService

/**
 *
 * @author caseycheng
 * @date 2023/10/15-14:16
 * @doc 这里存放所有管理员的命令
 **/
@JvmField
val adminCommands =
    createBotDispatcherModule("adminCommands", ::AdminCommandsConfiguration) { config ->
        val redisService = requireNotNull(config.redisService) { "require redis Service, please check" }
        val adminUserName = requireNotNull(config.adminUser) { "require admin username, please check" }
        AdminCommands(adminUserName, redisService)
    }

class AdminCommandsConfiguration {
    var adminUser: String? = null
    var redisService: RedisService? = null
}

fun AdminCommandsConfiguration.setAdminUsername(username: String) {
    this.adminUser = username
}

class AdminCommands(
    @Suppress("MemberVisibilityCanBePrivate")
    @PublishedApi
    internal val adminUserName: String,
    @PublishedApi
    internal val redisService: RedisService
) : BotDispatcher {
    override val dispatch: Dispatcher.() -> Unit = {
        //获取或建立管理员的档案
        adminCommand("set") {
            //set redis key
            val argsArray = args.toTypedArray()
            // key=user
            val key: String by commandArgs(argsArray)
            argsArray.asSequence()
        }
        adminCommand("del") {
            //del redis key
        }
    }
    override val dispatcherName: String
        get() = TODO("Not yet implemented")
    override val description: String
        get() = TODO("Not yet implemented")
}

context (AdminCommands)
private fun Dispatcher.adminCommand(
    command: String,
    commandHandlerEnvironment: CommandHandlerEnvironment.() -> Unit
) {
    val adminName = adminUserName

    command(command) {
        val user = message.from ?: return@command
        if (user.username != adminName) {
            return@command
        }
        commandHandlerEnvironment()
    }
}
