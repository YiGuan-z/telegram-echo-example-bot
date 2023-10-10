package module.redis

import com.fasterxml.jackson.databind.ObjectMapper
import io.lettuce.core.api.async.RedisAsyncCommands
import kotlinx.coroutines.future.await

/**
 *
 * @author caseycheng
 * @date 2023/10/3-00:01
 * @doc
 **/
class RedisService(val client: RedisAsyncCommands<String, Any>, val objMapper: ObjectMapper) {

    suspend inline fun <reified T : Any> get(path: String): T? {
        val data = client.get(path).await()
        return if (data == null) {
            null
        } else {
            objMapper.convertValue(data, jacksonTypeRef())
        }
    }

    suspend inline fun <reified T : Any> set(path: String, value: T): Boolean {
        val res = client.set(path, value).await()
        return res == "OK"
    }

}
