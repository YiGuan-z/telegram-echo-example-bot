package module.redis

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.kotlintelegrambot.entities.ChatId
import io.lettuce.core.api.async.RedisAsyncCommands
import kotlinx.coroutines.future.await
import module.bot.RedisKeys
import module.bot.modal.ChatLangProfile
import java.util.concurrent.TimeUnit

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

    suspend fun <T : Any> setEntry(redisEntry: RedisEntry<T>) {
        if (redisEntry.exprTime == -1L) {
            client.set(redisEntry.key, redisEntry.value).await()
        } else {
            client.setex(redisEntry.key, redisEntry.unit.toSeconds(redisEntry.exprTime), redisEntry.value).await()
        }
    }

    suspend inline fun <reified T : Any> getEntry(path: String): RedisEntry<T> {
        val data = client.get(path).await()
        return if (data == null) {
            RedisEntry.empty()
        } else {
            val ttl = client.ttl(path).await()
            RedisEntry(
                path,
                objMapper.convertValue(data, jacksonTypeRef()),
                ttl,
                TimeUnit.SECONDS
            )
        }
    }


}

fun <T : Any> redisEntryBuilder(
    key: String,
    value: T?,
    exprTime: Long = -1,
    unit: TimeUnit = TimeUnit.MINUTES
): RedisEntry<T> {
    return RedisEntry(key, value, exprTime, unit)
}

class RedisEntry<T : Any>(
    val key: String,
    val value: T?,
    val exprTime: Long = -1,
    val unit: TimeUnit = TimeUnit.MINUTES
) {
    companion object {
        @JvmStatic
        val EMPTY = RedisEntry("empty_in_empty", null)

        @Suppress("unchecked_cast")
        fun <T : Any> empty(): RedisEntry<T> {
            return EMPTY as RedisEntry<T>
        }

        fun isEmpty(entry: RedisEntry<*>): Boolean = entry === EMPTY
    }
}

suspend fun RedisService.currentChatLangProfile(id: ChatId.Id): ChatLangProfile? {
    return get<ChatLangProfile>("${RedisKeys.userChatKeys}:${id.id}")
}

suspend fun RedisService.setChatLangProfile(id: ChatId.Id, profile: ChatLangProfile) {
    set("${RedisKeys.userChatKeys}:${id.id}", profile)
}