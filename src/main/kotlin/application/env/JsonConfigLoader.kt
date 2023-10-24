package application.env

import com.fasterxml.jackson.databind.ObjectMapper
import module.redis.readValue
import module.thisLogger
import java.io.File

/**
 *
 * @author caseycheng
 * @date 2023/10/23-21:40
 * @doc
 **/
internal const val DEFAULT_JSON_FILE = "bot.json"

class JsonConfigLoader : ConfigLoader {
    override fun load(path: String?): ApplicationConfig? {
        return JsonConfig(path)?.apply { checkConfig() }
    }
}

class JsonConfig : ApplicationConfig {
    private val conf: Map<String, Any>
    private var root: JsonConfig = this

    constructor(data: Map<String, Any>) {
        this.conf = data
    }

    constructor(data: Map<String, Any>, root: JsonConfig) {
        this.conf = data
        this.root = root
    }

    fun checkConfig() {

    }


    override fun property(path: String): ApplicationConfigValue {
        return propertyOrNull(path) ?: throw ApplicationConfigurationException("Property $path not found")
    }

    override fun propertyOrNull(path: String): ApplicationConfigValue? {
        val paths = path.split('.')
        val map = paths.dropLast(1).fold(conf) { data, pat ->
            @Suppress("unchecked_cast")
            data[pat] as? Map<String, Any> ?: return null
        }
        val value = map[paths.last()] ?: return null
        return when (value) {
            is String -> resolveValue(value, root)?.let { JsonLiteralValue(path, it) }
            is Int -> resolveValue(value.toString(), root)?.let { JsonLiteralValue(path, it) }
            is List<*> -> {
                //String int
                val list = value.mapNotNullTo(arrayListOf()) {
                    if (it != null)
                        resolveValue(it.toString(), root)
                    else
                        null
                }
                return JsonListValue(path, list)
            }

            else -> throw ApplicationConfigurationException("Expected a literal or a list, but got $value")
        }
    }

    override fun config(path: String): ApplicationConfig {
        val parts = path.split('.')
        val jsonConf = parts.fold(conf) { map, part ->
            @Suppress("unchecked_cast")
            map[part] as? Map<String, Any>
                ?: throw ApplicationConfigurationException("$part is not a valid config path")
        }
        return JsonConfig(jsonConf, root)
    }

    @Suppress("UNCHECKED_CAST")
    override fun configList(path: String): List<ApplicationConfig> {
        val parts = path.split('.')
        val conf = parts.dropLast(1).fold(conf) { map, pat ->
            map[pat] as? Map<String, Any> ?: throw ApplicationConfigurationException("$pat is not found")
        }

        val values = conf[parts.last()] as? Map<String, Any>
            ?: throw ApplicationConfigurationException("$parts.last() is not a map")
        return values.map { (_, value) ->
            JsonConfig(
                value as? Map<String, Any> ?: throw ApplicationConfigurationException("$value is not a map"),
                root
            )
        }
    }

    override fun keys(): Set<String> {
        @Suppress("unchecked_cast")
        fun keys(map: Map<String, Any>): Set<String> {
            return map.keys.flatMap { key ->
                when (val value = map[key]) {
                    is Map<*, *> -> keys(value as Map<String, Any>).map { "$key.$it" }
                    else -> listOf(key)
                }
            }.toSet()
        }
        return keys(conf)
    }

    @Suppress("unchecked_cast")
    override fun toMap(): Map<String, Any?> {
        fun toPrimitive(element: Any?): Any? = when (element) {
            is List<*> -> element.map { toPrimitive(it) }
            is Map<*, *> -> {
                element as Map<String, Any>
                element.keys.associateWith { toPrimitive(element[it]) }
            }

            is String -> resolveValue(element, root)
            else -> null
        }

        val primitive = toPrimitive(conf)
        return primitive as? Map<String, Any?> ?: throw IllegalArgumentException("Top level element is not a map")
    }

    internal class JsonListValue(
        private val key: String,
        private val values: List<String>
    ) : ApplicationConfigValue {
        override fun getString(): String {
            throw ApplicationConfigurationException("Can't from $key get a string from a list")
        }

        override fun getList(): List<String> = values
    }

    internal class JsonLiteralValue(
        private val key: String,
        private val value: String
    ) : ApplicationConfigValue {
        override fun getString(): String = value

        override fun getList(): List<String> {
            throw ApplicationConfigurationException("Can't from $key get a list from a literal")
        }
    }
}


fun JsonConfig(path: String?): JsonConfig? {
    val resolverPath = when {
        path == null -> DEFAULT_JSON_FILE
        path.endsWith(".json") -> path
        else -> return null
    }

    val resource = Thread.currentThread().contextClassLoader.getResource(resolverPath)
    if (resource != null) {
        return resource.openStream().use {
            configFormJson(String(it.readBytes()))
        }
    }
    val file = File(resolverPath)

    return if (file.exists()) {
        configFormJson(file.readText())
    } else {
        null
    }
}

fun configFormJson(jsonText: String): JsonConfig {
    val mapper = ObjectMapper()
    val map = mapper.readValue<Map<String, Any>>(jsonText)
    // map int string
    thisLogger<JsonConfig>().info("json data is: \n {}", map)
    return JsonConfig(map)
}