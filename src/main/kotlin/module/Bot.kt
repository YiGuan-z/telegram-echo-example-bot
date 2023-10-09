package module

import application.createAppPlugin
import com.github.kotlintelegrambot.Bot

/**
 *
 * @author caseycheng
 * @date 2023/10/8-01:33
 * @doc
 **/
val bot = createAppPlugin("bot", Bot::Builder) { config ->
    config.build()
}