package github.cheng.application.env

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CommandArgsImplTest{
    val args = arrayOf("config=/etc/redis","redisUrl=localhost")
    @Test
    fun argsShouldBeResolvedConfigOptionNotNull(){
        val config:String by commandArgs(args)
        assertEquals("/etc/redis",config)
        val redisUrl:String by commandArgs(args)
        assertEquals("localhost",redisUrl)
    }
}