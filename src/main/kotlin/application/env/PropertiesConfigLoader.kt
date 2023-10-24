package application.env

import java.io.File
import java.util.*

/**
 *
 * @author caseycheng
 * @date 2023/10/21-02:40
 * @doc
 **/
internal const val DEFAULT_PROPERTIES_FILE = "bot.properties"

class PropertiesConfigLoader : ConfigLoader {
    override fun load(path: String?): ApplicationConfig? {
        return PropertiesConfig(path)?.apply { checkConfig() }
    }
}

open class PropertiesConfig : ApplicationConfig {
    private val properties: Properties
    private val path: String

    constructor(properties: Properties, path: String) {
        this.properties = properties
        this.path = path
    }

    constructor(properties: Properties) : this(properties, "")


    fun checkConfig() {

        TODO()
    }

    override fun property(path: String): ApplicationConfigValue {
        return PropertiesValue(properties, path)
    }

    override fun propertyOrNull(path: String): ApplicationConfigValue? {
        return if (properties.containsKey(path)) {
            PropertiesValue(properties, path)
        } else {
            null
        }
    }

    override fun config(path: String): ApplicationConfig {
        TODO("Not yet implemented")
        //rename property
    }

    override fun configList(path: String): List<ApplicationConfig> {
        TODO("Not yet implemented")
    }

    override fun keys(): Set<String> {
        TODO("Not yet implemented")
    }

    override fun toMap(): Map<String, Any?> {
        TODO("Not yet implemented")
    }
}

internal class PropertiesValue(val properties: Properties, val path: String) : ApplicationConfigValue {
    override fun getString(): String {
        return properties.getProperty(path)
    }

    override fun getList(): List<String> {
        val result = mutableListOf<String>()
        for (key in properties.keys) {
            key as String
            if (!key.startsWith(path)) {
                continue
            } else {
                if (!key.startsWith('[')) continue
                if (!key.endsWith(']')) continue
                result.add(properties.getProperty(key))
            }
        }
        return result
    }
}

//没有init?
fun PropertiesConfig(path: String?): PropertiesConfig? {
    val filePath = when {
        path == null -> return null
        path.endsWith(".properties") -> path
        else -> return null
    }

    val propertiesStream = Thread.currentThread().contextClassLoader.getResourceAsStream(filePath)
    if (propertiesStream != null) {
        val properties = Properties()
        properties.load(propertiesStream)
        return PropertiesConfig(properties)
    }
    val file = File(filePath)
    return if (file.exists()) {
        val properties = Properties()
        properties.load(file.inputStream())
        PropertiesConfig(properties)
    } else {
        null
    }
}