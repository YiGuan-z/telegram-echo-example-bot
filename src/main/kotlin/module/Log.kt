package module

import application.createAppPlugin
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 *
 * @author caseycheng
 * @date 2023/10/9-20:29
 * @doc
 **/
inline fun <reified C : Any> thisLogger(): Logger = LoggerFactory.getLogger(C::class.java)

inline fun <reified T : Any> T.logger(): Logger = thisLogger<T>()
