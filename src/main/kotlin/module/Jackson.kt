package module

import application.BotDSL
import application.createAppPlugin
import com.fasterxml.jackson.databind.ObjectMapper

/**
 *
 * @author caseycheng
 * @date 2023/10/8-12:41
 * @doc
 **/

val jackson = createAppPlugin("jackson", ::ObjectMapperConfig) { config ->
    return@createAppPlugin ObjectMapper().apply<ObjectMapper> { config.objSetting?.invoke(this) }.apply { logger().info("jackson is ready") }
}

typealias objSetting = ObjectMapper.() -> Unit

@BotDSL
class ObjectMapperConfig {
    internal var objSetting: objSetting? = null

    fun jacksonConfig(setting: objSetting) {
        objSetting = setting
    }

}