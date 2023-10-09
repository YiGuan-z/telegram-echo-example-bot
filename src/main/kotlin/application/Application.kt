import chat.ApplicationPluginInstance
import chat.AttributeKey
import chat.ChatHandler
import chat.handler.EchoChatHandler
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.text
import com.github.kotlintelegrambot.entities.ChatId
import module.handleStickersImage

/**
 *
 * @author caseycheng
 * @date 2023/10/3-00:06
 * @doc
 **/
class Application {
    fun <Config : Any, Plugin : Any> install(
        plugin: ApplicationPluginInstance<Config, Plugin>,
        config: Config.() -> Unit = {}
    ): Plugin {
        return plugin.install(config)
    }

    fun configBot(): Bot {
        //链接redis
        val bot = bot {
            token = env("TG_BOT_TOKEN")
            dispatch {
                text {
                    val (_, update, message, text) = this
                    //为每个用户都设定不同的模式
                    var handler = ChatHandler.getInstance(message.chat.id)
                    if (handler == null) {
                        //默认的消息处理器为复读机模式
                        ChatHandler.setInstance(message.chat.id, EchoChatHandler)
                        handler = ChatHandler.getInstance(message.chat.id)
                    }
                    //这里已经被处理成不会为空了
                    handler?.handle(message, text, update)
                }
                command("start") {
                    bot.sendMessage(ChatId.fromId(message.chat.id), "Hello, world!")
                }
                command("images") {
                    handleStickersImage()
                }
            }
        }
//        ChatHandler.install(arrayOf(EchoChatHandler))
        ChatHandler.bot = bot
        return bot
    }
}

fun <PlugConfig : Any, Plugin : Any> createApplicationPlugin(
    name: String,
    createConfiguration: () -> PlugConfig,
    createPlugin: (config: PlugConfig) -> Plugin
): ApplicationPluginInstance<PlugConfig, Plugin> = object :
    ApplicationPluginInstance<PlugConfig, Plugin> {
    override val attributeKey: AttributeKey = AttributeKey(name)

    override fun install(configuration: PlugConfig.() -> Unit): Plugin {
        val plugConfig = createConfiguration().apply(configuration)
        return createPlugin(plugConfig)
    }
}