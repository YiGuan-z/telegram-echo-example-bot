package module.bot.modal

import GlobalResource

/**
 *
 * @author caseycheng
 * @date 2023/10/14-17:16
 * @doc
 **/
data class ChatLangProfile(
    val chatId: Long = -1,
    val lang: String = GlobalResource.defaultLang
)