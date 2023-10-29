package github.cheng.application

import com.github.kotlintelegrambot.dispatcher.Dispatcher

/**
 *
 * @author caseycheng
 * @date 2023/10/3-00:58
 * @doc
 **/
data class AttributeKey(val key: String)

interface Plugin<Config : Any, PluginT : Any> {
    val attributeKey: AttributeKey

    fun install(configuration: Config.() -> Unit): PluginT
}

interface ApplicationPlugin<Config : Any, PluginT : Any> : Plugin<Config, PluginT> {
    val feature: Feature
}

interface ApplicationPluginInstance<Config : Any, PluginT : Any> : ApplicationPlugin<Config, PluginT>

/**
 * 表示可以安装到的命名空间
 */
enum class Feature {
    Command,
    App,
    BotDispatcher,
}

// ------------------------------------------BOT-----------------------------------------
interface BotDispatcher {
    val dispatch: Dispatcher.() -> Unit
    val dispatcherName: String
    val description: String
}

interface BotDispatcherModule<ConfigT : Any, out DispatcherT : BotDispatcher> :
    ApplicationPlugin<ConfigT, @UnsafeVariance DispatcherT>
