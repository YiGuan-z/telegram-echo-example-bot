package github.cheng.application.env

import java.util.*

/**
 *
 * @author caseycheng
 * @date 2023/10/4-15:57
 * @doc
 **/
interface ApplicationConfig {
    fun property(path: String): ApplicationConfigValue
    fun propertyOrNull(path: String): ApplicationConfigValue?
    fun config(path: String): ApplicationConfig
    fun configList(path: String): List<ApplicationConfig>
    fun keys(): Set<String>

    //linkedHashMap
    fun toMap(): Map<String, Any?>
}

interface ApplicationConfigValue {
    fun getString(): String
    fun getList(): List<String>
}

fun ApplicationConfigValue.getStringOrNull() = runCatching { getString() }.getOrNull()
fun ApplicationConfigValue.getListOrNull() = runCatching { getList() }.getOrNull()


open class MapApplicationConfig : ApplicationConfig {
    protected val map: MutableMap<String, String>
    protected val path: String

    constructor(map: MutableMap<String, String>, path: String) {
        this.map = map
        this.path = path
    }

    constructor(values: List<Pair<String, String>>) : this(values.toMap().toMutableMap(), "") {
        val elements = mutableMapOf<String, Int>()
        for (value in values) findListElements(value.first, elements)
        elements.forEach { (property, size) -> this.map["$property.size"] = "$size" }
    }

    constructor() : this(mutableMapOf(), "")

    fun put(path: String, value: String) {
        map[combine(this.path, path)] = value
    }

    fun put(path: String, values: Iterable<String>) {
        var size = 0
        values.forEachIndexed { index, value ->
            put(combine(path, index.toString()), value)
            size++
        }
        put(combine(path, "size"), size.toString())
    }

    override fun property(path: String): ApplicationConfigValue {
        return propertyOrNull(path) ?: throw ApplicationConfigurationException("Property $path not found")
    }

    override fun propertyOrNull(path: String): ApplicationConfigValue? {
        val key = combine(this.path, path)
        return if (!map.containsKey(key) && !map.containsKey(combine(key, "size"))) {
            null
        } else {
            MapApplicationConfigValue(map, key)
        }
    }


    override fun config(path: String): ApplicationConfig {
        return MapApplicationConfig(map, combine(this.path, path))
    }

    override fun configList(path: String): List<ApplicationConfig> {
        val key = combine(this.path, path)
        val size = map[combine(key, "size")] ?: throw ApplicationConfigurationException("property $key.size not found")
        return (0..<size.toInt()).map { MapApplicationConfig(map, combine(key, it.toString())) }
    }

    override fun keys(): Set<String> {
        val isRoot = path.isEmpty()
        //过滤出当前path下的所有路径
        val keys = if (isRoot) map.keys else map.keys.filter { it.startsWith("$path.") }
        //列出size列表
        val listEntries = keys.filter { it.contains(".size") }.map { it.substringBefore(".size") }
        val addedListKeys = mutableSetOf<String>()
        return keys.mapNotNull { candidate ->
            val listKey = listEntries.firstOrNull { candidate.startsWith(it) }
            val key = when {
                listKey != null && !addedListKeys.contains(listKey) -> {
                    addedListKeys.add(listKey)
                    listKey
                }

                listKey == null -> candidate
                else -> null
            }
            if (isRoot) key else key?.substringAfter("$path.")
        }.toSet()
    }

    override fun toMap(): Map<String, Any?> {
        val keys = map.keys.filter { it.startsWith(path) }
            //if there is a profile, discard and split it, take out the first element when splitting, aka key
            .map { it.drop(if (path.isEmpty()) 0 else path.length + 1).split('.').first() }
            .distinct()
        return keys.associate { key ->
            val path = combine(path, key)
            when {
                map.containsKey(path) -> key to map[path]
                map.containsKey(combine(path, "size")) -> when {
                    map.containsKey(combine(path, "0")) -> key to property(path).getList()
                    else -> key to configList(path).map { it.toMap() }
                }

                else -> key to config(path).toMap()
            }
        }
    }

    class MapApplicationConfigValue(val map: Map<String, String>, val path: String) :
        ApplicationConfigValue {
        override fun getString(): String {
            return map[path]!!
        }

        override fun getList(): List<String> {
            val size =
                map[combine(path, "size")] ?: throw ApplicationConfigurationException("Property $path.size not found")
            return (0..<size.toInt()).map { map[combine(path, it.toString())]!! }
        }

    }
}

class MergedApplicationConfig(
    val first: ApplicationConfig,
    val second: ApplicationConfig
) : ApplicationConfig {
    val firstKeys by lazy { first.keys() }
    val secondKeys by lazy { second.keys() }

    override fun property(path: String): ApplicationConfigValue = when {
        path in firstKeys -> first.property(path)
        else -> second.property(path)
    }

    override fun propertyOrNull(path: String): ApplicationConfigValue? = when {
        path in firstKeys -> first.propertyOrNull(path)
        else -> second.propertyOrNull(path)
    }

    override fun config(path: String): ApplicationConfig {
        if (firstKeys.none { it.startsWith("$path.") }) return second.config(path)
        if (secondKeys.none { it.startsWith("$path.") }) return first.config(path)
        return MergedApplicationConfig(first.config(path), second.config(path))
    }

    override fun configList(path: String): List<ApplicationConfig> {
        val firstConfigList = if (path in firstKeys) first.configList(path) else emptyList()
        val secondConfigList = if (path in secondKeys) second.configList(path) else emptyList()
        return firstConfigList + secondConfigList
    }

    override fun keys(): Set<String> = firstKeys + secondKeys


    override fun toMap(): Map<String, Any?> = first.toMap() + second.toMap()

}

fun ApplicationConfig.mergeWith(other: ApplicationConfig): ApplicationConfig {
    return MergedApplicationConfig(other, this)
}

private fun findListElements(input: String, elements: MutableMap<String, Int>) {
    var pointBegin = input.indexOf('.')
    while (pointBegin != input.length) {
        val pointEnd = input.indexOf('.', pointBegin + 1).let {
            if (it == -1) {
                input.length
            } else {
                it
            }
        }
        input.substring(pointBegin + 1, pointEnd).toIntOrNull()?.let { pos ->
            val element = input.substring(0, pointBegin)
            val newSize = pos + 1
            elements[element] = elements[element]?.let { maxOf(it, newSize) } ?: newSize
        }
        pointBegin = pointEnd
    }
}

private fun combine(root: String, relative: String): String = if (root.isEmpty()) {
    relative
} else {
    "$root.$relative"
}

interface ConfigLoader {
    fun load(path: String? = null): ApplicationConfig?

    companion object Default {
        fun Default.load(path: String? = null): ApplicationConfig {
            if (path == null) {
                val defaultConfig = loadDefault()
                if (defaultConfig != null) return defaultConfig
            }
            for (loader in configLoaders) {
                val applicationConfig = loader.load(path)
                if (applicationConfig != null) return applicationConfig
            }
            return MapApplicationConfig()
        }

        private fun loadDefault(): ApplicationConfig? {
            for (path in configPath) {
                for (loader in configLoaders) {
                    val config = loader.load(path)
                    if (config != null) return config
                }
            }
            return null
        }
    }
}

class ApplicationConfigurationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

//获取配置的加载器
private val configLoaders: List<ConfigLoader> = ConfigLoader::class.java.let {
    ServiceLoader.load(it, it.classLoader).toList()
}

private val configPath: List<String>
    get() = buildList {
        System.getProperty("config.file")?.let { add(it) }
        System.getProperty("config.resource")?.let { add(it) }
        System.getProperty("config.url")?.let { add(it) }
    }

