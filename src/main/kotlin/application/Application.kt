package application

import application.env.ApplicationConfig
import application.env.ConfigLoader
import application.env.ConfigLoader.Default.load
import application.env.MapApplicationConfig
import application.env.mergeWith
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.logging.LogLevel
import env
import module.bot

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

    fun <Config : Any, PluginA : Any> instance(pluginInstance: ApplicationPluginInstance<Config, PluginA>): PluginA {
        val attributeKey = pluginInstance.attributeKey
        val feature = pluginInstance.feature
        return instance(feature, attributeKey)
    }

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
            val environment = getConfigFromArgs(args, getConfigFromArgs(args))
            val application = Application(environment)
                .apply(block)
                .also { it.configBot() }
            application.instance(bot).startPolling()
        }

        private fun Application.configBot() {
            install(bot) {
                token = env("TG_BOT_TOKEN")
                logLevel = LogLevel.Error
                dispatch {
                    val dispatcher = this
                    botDispatcherModules.forEach { (_, botDispatcherModule) ->
                        botDispatcherModule.apply { dispatcher.dispatch() }
                    }
                }
            }
        }


    }
}

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

internal fun getConfigFromArgs(args: Array<String>): ApplicationConfig =
    if (args.isEmpty()) {
        val jvmEnvironment = getConfigFormJvmEnvironment()
        val systemEnvironment = getConfigFormSystemEnvironment()
        systemEnvironment.mergeWith(jvmEnvironment)
    } else {
        buildEnvironment(args)
    }

internal fun getConfigFromArgs(args: Array<String>, environmentConfig: ApplicationConfig): ApplicationConfig =
    if (args.isEmpty()) {
        environmentConfig
    } else {
        buildEnvironment(args)
    }

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


