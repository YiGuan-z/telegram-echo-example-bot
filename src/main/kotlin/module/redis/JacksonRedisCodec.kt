package module.redis

import io.lettuce.core.codec.RedisCodec
import setOnce
import java.io.Reader
import java.nio.ByteBuffer

/**
 *
 * @author caseycheng
 * @date 2023/10/8-12:50
 * @doc
 **/
class JacksonRedisCodec(config: GsonRedisCodecConfig) : RedisCodec<String, Any> {
    private val mapper = config.mapper

    override fun decodeKey(bytes: ByteBuffer): String {
        return mapper.fromJson(bytes.getByte(),String::class.java)
    }

    override fun decodeValue(bytes: ByteBuffer): Any {
        TODO("Not yet implemented")
    }

    override fun encodeKey(key: String): ByteBuffer {
        TODO("Not yet implemented")
    }

    override fun encodeValue(value: Any): ByteBuffer {
        TODO("Not yet implemented")
    }

    companion object {
        private fun ByteBuffer.getByte(): ByteArray {
            val byteArray = ByteArray(this.remaining())
            get(byteArray)
            return byteArray
        }
    }
}

class GsonRedisCodecConfig {
    var mapper: Gson by setOnce()
}
