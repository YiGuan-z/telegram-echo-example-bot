package application.env

import envOfNullable
import net.mamoe.yamlkt.*
import java.io.File
import java.lang.IllegalArgumentException


/**
 *
 * @author caseycheng
 * @date 2023/10/11-14:06
 * @doc
 **/
internal const val DEFAULT_YAML_FILE = "bot.yaml"

class YamlConfigLoader : ConfigLoader {
    override fun load(path: String?): ApplicationConfig? {
        return YamlConfig(path)
    }
}

class YamlConfig(
    private val yamlMap: YamlMap
) : ApplicationConfig {
    private var root: YamlConfig = this

    constructor(yaml: YamlMap, root: YamlConfig) : this(yaml) {
        this.root = root
    }

    override fun property(path: String): ApplicationConfigValue {
        return propertyOrNull(path) ?: throw ApplicationConfigurationException("$path is not found")
    }

    override fun propertyOrNull(path: String): ApplicationConfigValue? {
        val paths = path.split('.')
        //cd到当前路径的上一级
        val yaml = paths.dropLast(1).fold(yamlMap) { yaml, pat -> yaml[pat] as? YamlMap ?: return null }
        //获取其中的当前行
        val value = yaml[paths.last()] ?: return null
        return when (value) {
            is YamlLiteral -> resolveValue(value.content, root)?.let { ConfigLiteralValue(path, it) }
            is YamlList -> {
                val list = value.content.map { element ->
                    element.asLiteralOrNull()?.content?.let { resolveValue(it, root) }
                        ?: throw ApplicationConfigurationException("$element is not a literal")
                }
                return ConfigListValue(path, list)
            }

            else -> throw ApplicationConfigurationException("Expected a literal or a list, but got $value")
        }
    }

    override fun config(path: String): ApplicationConfig {
        val parts = path.split('.')
        val yaml = parts.fold(yamlMap) { yaml, part ->
            yaml[part] as? YamlMap ?: throw ApplicationConfigurationException("$part is not found")
        }
        return YamlConfig(yaml, root)
    }

    override fun configList(path: String): List<ApplicationConfig> {
        val parts = path.split('.')
        val yaml = parts.dropLast(1).fold(yamlMap) { yaml, pat ->
            yaml[pat] as? YamlMap ?: throw ApplicationConfigurationException("$pat is not found")
        }
        val value =
            yaml[parts.last()] as? YamlList ?: throw ApplicationConfigurationException("Path $path is not found")

        return value.map {
            YamlConfig(
                it as? YamlMap ?: throw ApplicationConfigurationException("$it is not a map"),
                root
            )
        }

    }

    override fun keys(): Set<String> {
        fun keys(yaml: YamlMap): Set<String> =
            yaml.keys.map { it.content as String }
                .flatMap { key ->
                    when (val value = yaml[key]) {
                        is YamlMap -> keys(value).map { "$key.$it" }
                        else -> listOf(key)
                    }
                }
                .toSet()
        return keys(yamlMap)
    }

    override fun toMap(): Map<String, Any?> {
        fun toPrimitive(yaml: YamlElement?): Any? = when (yaml) {
            is YamlLiteral -> resolveValue(yaml.content, root)
            is YamlMap -> yaml.keys.associate { it.content as String to toPrimitive(yaml[it]) }
            is YamlList -> yaml.content.map { toPrimitive(it) }
            YamlNull -> null
            null -> null
        }

        val primitive = toPrimitive(yamlMap)
        @Suppress("unchecked_cast")
        return primitive as? Map<String,Any?> ?: throw IllegalArgumentException("Top level element is not a map")
    }

    class ConfigLiteralValue(private val key: String, private val value: String) : ApplicationConfigValue {
        override fun getString(): String = value
        override fun getList(): List<String> = throw ApplicationConfigurationException("Property $key is not a list")
    }

    class ConfigListValue(private val key: String, private val values: List<String>) : ApplicationConfigValue {
        override fun getString(): String = throw ApplicationConfigurationException("Property $key is not a string")
        override fun getList(): List<String> = values
    }
}

private fun resolveValue(value: String, root: YamlConfig): String? {
    //检查时候是环境变量
    val isEnvVariable = value.startsWith("\$")
    if (!isEnvVariable) return value
    val keyWithDefault = value.drop(1)
    //检查是否存在默认值
    val separatorIndex = keyWithDefault.indexOf(':')
    //如果存在默认值，则，返回环境变量或者默认值
    if (separatorIndex != -1) {
        val key = keyWithDefault.substring(0, separatorIndex)
        return envOfNullable(key) ?: keyWithDefault.drop(separatorIndex + 1)
    }
    //如果不存在默认值，则尝试从配置中获取并返回
    val selfReference = root.propertyOrNull(keyWithDefault)
    if (selfReference != null) {
        return selfReference.getString()
    }
    //如果不存在，那么就看看是否是可选的
    val isOptional = keyWithDefault.first() == '?'
    //获取可选的key
    val key = if (isOptional) keyWithDefault.drop(1) else keyWithDefault
    //从环境变量中返回它，不是可选的项将会抛出异常。
    return envOfNullable(key) ?: if (isOptional) {
        null
    } else {
        throw ApplicationConfigurationException("Environment variable $key is not defined")
    }
}

fun YamlConfig(path: String?): YamlConfig? {
    val resolverPath = when {
        path == null -> DEFAULT_YAML_FILE
        path.endsWith(".yaml") -> path
        else -> return null
    }
    val resource = Thread.currentThread().contextClassLoader.getResource(resolverPath)
    if (resource != null) {
        return resource.openStream().use {
            configFromString(String(it.readBytes()))
        }
    }
    val file = File(resolverPath)
    return if (file.exists()) {
        configFromString(file.readText())
    } else {
        null
    }

}

private fun configFromString(content: String): YamlConfig {
    val yaml = Yaml.decodeYamlFromString(content) as? YamlMap
        ?: throw ApplicationConfigurationException("Config should be a yaml")
    return YamlConfig(yaml)
}