package github.cheng.module.bot

import com.github.kotlintelegrambot.Bot
import github.cheng.application.createAppPlugin

/**
 *
 * @author caseycheng
 * @date 2023/10/8-01:33
 * @doc
 **/
val bot =
    createAppPlugin("bot", Bot::Builder) { config ->
        config.build()
    }
