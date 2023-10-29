package github.cheng.module.bot.modal

import github.cheng.TelegramResources
import github.cheng.application.Language

/**
 *
 * @author caseycheng
 * @date 2023/10/14-17:16
 * @doc
 **/
data class ChatLangProfile(
    val chatId: Long = -1,
    val lang: Language = Language.of(TelegramResources.defaultLang),
)
