package github.cheng.module.redis

import com.fasterxml.jackson.databind.ObjectMapper
import github.cheng.application.createAppPlugin
import github.cheng.getByte
import github.cheng.setOnce
import io.lettuce.core.codec.RedisCodec
import java.nio.ByteBuffer

/**
 *
 * @author caseycheng
 * @date 2023/10/8-12:50
 * @doc
 **/
class JacksonRedisCodec(config: JacksonRedisCodecConfig) : RedisCodec<String, Any> {
    private val mapper = config.mapper

    override fun decodeKey(bytes: ByteBuffer): String {
        return mapper.readValue<String>(bytes.getByte())
    }

    override fun decodeValue(bytes: ByteBuffer): Any {
        return mapper.readValue(bytes.getByte())
    }

    override fun encodeKey(key: String): ByteBuffer {
        return ByteBuffer.wrap(mapper.writeValueAsBytes(key))
    }

    override fun encodeValue(value: Any): ByteBuffer {
        return ByteBuffer.wrap(mapper.writeValueAsBytes(value))
    }
}

// @BotDSL
class JacksonRedisCodecConfig {
    var mapper: ObjectMapper by setOnce()
}

fun JacksonRedisCodecConfig.mapper(block: () -> ObjectMapper) {
    this.mapper = block()
}

val jacksonRedisCodec =
    createAppPlugin(
        "jacksonRedisCodec",
        ::JacksonRedisCodecConfig,
    ) { config ->
        return@createAppPlugin JacksonRedisCodec(config)
    }
