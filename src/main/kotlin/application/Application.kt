package application

import application.env.ApplicationConfig
import application.env.ConfigLoader
import application.env.ConfigLoader.Default.load
import application.env.MapApplicationConfig
import application.env.mergeWith
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.logging.LogLevel
import module.bot
import module.thisLogger
import org.slf4j.Logger

/**
 *
 * @author caseycheng
 * @date 2023/10/3-00:06
 * @doc
 **/
@BotDSL
class Application(applicationConfig: ApplicationConfig) {
    protected val appPlugins: MutableMap<AttributeKey, Any> = linkedMapOf()
    protected val commandPlugins: MutableMap<AttributeKey, Any> = linkedMapOf()
    protected val botDispatcherModules: MutableMap<AttributeKey, BotDispatcher> = linkedMapOf()
    val appEnvironment: ApplicationConfig = applicationConfig
    val logger: Logger = thisLogger<Application>()
    val isDebug = logger.isDebugEnabled

    @BotDSL
    fun <Config : Any, Plugin : Any> install(
        plugin: ApplicationPlugin<Config, Plugin>,
        configBuilder: Config.() -> Unit = {}
    ): Plugin {
        val feature = plugin.feature
        val attributeKey = plugin.attributeKey
        val pluginInstance = plugin.install(configBuilder)
        when (feature) {
            Feature.App -> putPlugin(appPlugins, attributeKey, pluginInstance)
            Feature.Command -> putPlugin(commandPlugins, attributeKey, pluginInstance)
            Feature.BotDispatcher -> putPlugin(
                botDispatcherModules,
                attributeKey,
                pluginInstance as? BotDispatcher ?: throw TypeCastException("必须实现BotDispatcher类型")
            )
        }
        return pluginInstance
    }

    @BotDSL
    fun <Config : Any, PluginA : Any> instance(pluginInstance: ApplicationPluginInstance<Config, PluginA>): PluginA {
        val attributeKey = pluginInstance.attributeKey
        val feature = pluginInstance.feature
        return instance(feature, attributeKey)
    }

    @BotDSL
    fun <PluginA> instance(feature: Feature, attributeKey: AttributeKey): PluginA {
        return when (feature) {
            Feature.App -> getPlugin(appPlugins, attributeKey)
            Feature.Command -> getPlugin(commandPlugins, attributeKey)
            Feature.BotDispatcher -> getPlugin(botDispatcherModules, attributeKey)
        }
    }


    private fun <V : Any> putPlugin(plugins: MutableMap<AttributeKey, V>, attributeKey: AttributeKey, plugin: V) {
        if (attributeKey in plugins) {
            throw IllegalStateException("Plugin $attributeKey already installed.")
        }
        plugins[attributeKey] = plugin
    }

    @Suppress("UNCHECKED_CAST")
    private fun <PluginA, V : Any> getPlugin(plugins: Map<AttributeKey, V>, attributeKey: AttributeKey): PluginA {
        if (attributeKey !in plugins) {
            throw IllegalStateException("Plugin $attributeKey not installed.")
        }
        return plugins[attributeKey] as PluginA
    }


    companion object {
        fun main(args: Array<String>, block: Application.() -> Unit) {
            val environment = buildApplicationConfig(args)
            val application = Application(environment)
                .apply(init)
                .apply(block)
                .apply(configBot)
            application.instance(bot).startPolling()
                .also { thisLogger<Application>().info("机器人已启动") }
        }


        private val init: Application.() -> Unit = {
            //打印banner
            logger.info("application正在启动")
            banner?.let { logger.info(it) }
        }

        private val banner: String? =
            runCatching {
                this::class.java.classLoader.getResourceAsStream("banner.txt")?.readBytes()?.readToString()
            }.getOrNull()


        private val configBot: Application.() -> Unit = {
            install(bot) {
//                token = env("TG_BOT_TOKEN")
                token = appEnvironment.config("bot").property("tg_token").getString()
                logLevel = LogLevel.Error
                dispatch {
                    val dispatcher = this
                    botDispatcherModules.forEach { (_, botDispatcherModule) ->
                        botDispatcherModule.apply { dispatcher.dispatch() }
                        logger.info("Bot Dispatcher Module loaded: {}", botDispatcherModule.dispatcherName)
                        logger.info("Bot Dispatcher Module description: {}", botDispatcherModule.description)
                    }
                }
            }
        }


    }
}

private fun ByteArray.readToString() = String(this)

private fun buildEnvironment(args: Array<String>): ApplicationConfig {
    val list = args.mapNotNull { it.splitPair('=') }
    val configPaths = list.filter { it.first == "-config" }.map { it.second }
    val environmentConfig = getConfigFormJvmEnvironment()
    val systemEnvironment = getConfigFormSystemEnvironment()
    val config = when (configPaths.size) {
        0 -> ConfigLoader.load()
        1 -> ConfigLoader.load(configPaths.single())
        else -> configPaths.map { ConfigLoader.load(it) }.reduce { first, second -> first.mergeWith(second) }
    }
    return environmentConfig.mergeWith(config).mergeWith(MapApplicationConfig(list)).mergeWith(systemEnvironment)
}

internal fun getConfigFormJvmEnvironment(): ApplicationConfig = System.getProperties()
    .toMap()
    .let { env -> MapApplicationConfig(env.map { it.key as String to it.value as String }) }

internal fun getConfigFormSystemEnvironment(): ApplicationConfig =
    MapApplicationConfig(System.getenv().map { it.key as String to it.value as String })

internal fun buildApplicationConfig(args: Array<String>): ApplicationConfig =
        buildEnvironment(args)

internal fun String.splitPair(separator: Char): Pair<String, String>? {
    val index = indexOf(separator)
    return if (index > 0) {
        Pair(take(index), drop(index + 1))
    } else {
        null
    }
}

@BotDSL
fun <PlugConfig : Any, Plugin : Any> createAppPlugin(
    name: String,
    createConfiguration: () -> PlugConfig,
    pluginBuilder: (config: PlugConfig) -> Plugin
): ApplicationPluginInstance<PlugConfig, Plugin> =
    createPlugin(name, Feature.App, createConfiguration, pluginBuilder)

@BotDSL
fun <PlugConfig : Any, PluginT : BotDispatcher> createBotDispatcherModule(
    name: String,
    createConfiguration: () -> PlugConfig,
    pluginBuilder: (config: PlugConfig) -> PluginT
): ApplicationPluginInstance<PlugConfig, PluginT> =
    createPlugin(name, Feature.BotDispatcher, createConfiguration, pluginBuilder)

@BotDSL
fun <PlugConfig : Any, Plugin : Any> createPlugin(
    name: String,
    feature: Feature,
    createConfiguration: () -> PlugConfig,
    pluginBuilder: (config: PlugConfig) -> Plugin
): ApplicationPluginInstance<PlugConfig, Plugin> = object :
    ApplicationPluginInstance<PlugConfig, Plugin> {

    override val attributeKey: AttributeKey = AttributeKey(name)
    override val feature: Feature = feature

    override fun install(configuration: PlugConfig.() -> Unit): Plugin {
        val plugConfig = createConfiguration().apply(configuration)
        return pluginBuilder(plugConfig)
    }

}


