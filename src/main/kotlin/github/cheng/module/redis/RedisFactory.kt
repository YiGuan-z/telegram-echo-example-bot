package github.cheng.module.redis

import github.cheng.application.createAppPlugin
import io.lettuce.core.RedisClient
import io.lettuce.core.codec.RedisCodec
import github.cheng.setOnce
import java.nio.ByteBuffer

/**
 *
 * @author caseycheng
 * @date 2023/10/2-21:34
 * @doc
 **/
class RedisFactory(val conf: RedisConfiguration) {

    fun newClient(): RedisClient = RedisClient.create(conf.url)

    companion object Feature {
        lateinit var factory: RedisFactory
        val client by lazy { factory.newClient() }

        fun <K : Any, V : Any> newConnection(
            keyEncoder: (K) -> ByteBuffer,
            keyDecoder: (ByteBuffer) -> K,
            valueEncoder: (V) -> ByteBuffer,
            valueDecoder: (ByteBuffer) -> V
        ) = client.connect(
            createCodec(
                keyEncoder,
                keyDecoder,
                valueEncoder,
                valueDecoder
            )
        )

        fun <K : Any, V : Any> createCodec(
            keyEncoder: (K) -> ByteBuffer,
            keyDecoder: (ByteBuffer) -> K,
            valueEncoder: (V) -> ByteBuffer,
            valueDecoder: (ByteBuffer) -> V
        ) = object : RedisCodec<K, V> {
            override fun decodeKey(bytes: ByteBuffer): K = keyDecoder(bytes)
            override fun decodeValue(bytes: ByteBuffer): V = valueDecoder(bytes)
            override fun encodeKey(key: K): ByteBuffer = keyEncoder(key)
            override fun encodeValue(value: V): ByteBuffer = valueEncoder(value)
        }

        fun <K : Any, V : Any> newConnection(codec: RedisCodec<K, V>) = client.connect(codec)
        fun <K : Any, V : Any> newAsyncClient(codec: RedisCodec<K, V>) = newConnection(codec).async()
        fun <K : Any, V : Any> newSyncClient(codec: RedisCodec<K, V>) = newConnection(codec).sync()
        fun <K : Any, V : Any> newClient(codec: RedisCodec<K, V>) = newConnection(codec).sync()
        fun <K : Any, V : Any> newReactiveClient(codec: RedisCodec<K, V>) = newConnection(codec).reactive()
    }
}

class RedisConfiguration {
    var url: String by setOnce()
}

val redisFactory = createAppPlugin("redisFactory", ::RedisConfiguration) { config ->
    RedisFactory(config).apply { RedisFactory.factory = this }
}