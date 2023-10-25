package github.cheng.application.env

import github.cheng.envOfNullable

/**
 *
 * @author caseycheng
 * @date 2023/10/24-11:44
 * @doc
 **/
internal fun resolveValue(value: String, root: ApplicationConfig): String? {
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

