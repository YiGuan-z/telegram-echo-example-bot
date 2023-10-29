package github.cheng.application.env

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.system.measureNanoTime

class CommandArgsImplTest {
    @JvmField
    val args = arrayOf("config=/etc/redis", "redisUrl=localhost")

    @JvmField
    val ar1 = arrayOf("config=/conf.json", "remoteUrl=localhost")

    @JvmField
    val ar2 = arrayOf("config=/conf.json", "remoteUrl=localhost")

    @Test
    fun argsShouldBeResolvedConfigOptionNotNull() {
        val n1 = measureNanoTime {
            val config: String by commandArgs(args)
            val redisUrl: String by commandArgs(args)
            assertEquals("localhost", redisUrl)
            assertEquals("/etc/redis", config)
        }
        println(n1)
        val n2 = measureNanoTime {
            val config: String by commandArgs(ar1)
            val remoteUrl: String by commandArgs(ar2)
            assertEquals("localhost", remoteUrl)
            assertEquals("/conf.json", config)
        }
        println(n2)
    }

    @Test
    fun testArrayString() {
        val array1 = arrayOf("a", "b", "c")
        val array2 = arrayOf("a", "b", "c")
        println(array1.contentDeepHashCode())
        println(array2.contentDeepHashCode())
        array1[0] = array1[1]
        println(array1.contentDeepHashCode())
        println(array2.contentDeepHashCode())

    }
}
