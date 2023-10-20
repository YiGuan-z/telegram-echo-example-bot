package module

import application.BotDSL
import application.createAppPlugin
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper

/**
 *
 * @author caseycheng
 * @date 2023/10/8-12:41
 * @doc
 **/

val jackson = createAppPlugin("jackson", ::ObjectMapperConfig) { config ->
    return@createAppPlugin ObjectMapper().apply<ObjectMapper> {
        config.objectSettings.forEach { it() }
    }.apply { logger().info("jackson is ready") }
}

typealias objSetting = ObjectMapper.() -> Unit

@BotDSL
class ObjectMapperConfig {
    internal var objectSettings: MutableList<objSetting> = mutableListOf()

    fun jacksonConfig(setting: objSetting) {
        objectSettings.add(setting)
    }

}

@BotDSL
fun ObjectMapperConfig.ignoreUnknownProperties(ignore: Boolean) {
    jacksonConfig {
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, !ignore)
    }
}