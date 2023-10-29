package github.cheng.module.redis

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper

/**
 *
 * @author caseycheng
 * @date 2023/10/8-16:32
 * @doc
 **/
inline fun <reified T> jacksonTypeRef(): TypeReference<T> = object : TypeReference<T>() {}

inline fun <reified T> ObjectMapper.readValue(json: String): T = readValue(json, jacksonTypeRef<T>())

inline fun <reified T> ObjectMapper.readValue(array: ByteArray): T = readValue(array, jacksonTypeRef<T>())
