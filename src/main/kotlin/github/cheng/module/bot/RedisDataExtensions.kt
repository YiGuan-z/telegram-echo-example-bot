package github.cheng.module.bot

import com.github.kotlintelegrambot.entities.ChatId
import github.cheng.module.bot.modal.ChatLangProfile
import github.cheng.module.bot.modal.StickerCollectPack
import github.cheng.module.redis.RedisService

/**
 *
 * @author caseycheng
 * @date 2023/10/25-17:21
 * @doc
 **/
/*--------------------------------------ChatLangProfile------------------------------------------*/
suspend fun RedisService.currentChatLangProfile(id: ChatId.Id): ChatLangProfile? {
    return get<ChatLangProfile>("${RedisKeys.userChatKeys}:${id.id}")
}

suspend fun RedisService.setChatLangProfile(id: ChatId.Id, profile: ChatLangProfile) {
    set("${RedisKeys.userChatKeys}:${id.id}", profile)
}
/*--------------------------------------END------------------------------------------*/

/*--------------------------------------StickerCollectPack------------------------------------------*/
suspend fun RedisService.getCurrentPack(id: ChatId.Id): StickerCollectPack? {
    return get("${RedisKeys.newPack}:${id.id}")
}

suspend fun RedisService.setCurrentPack(id: ChatId.Id, pack: StickerCollectPack) {
    set("${RedisKeys.newPack}:${id.id}", pack)
}

suspend fun RedisService.removeCurrentPack(id: ChatId.Id) {
    remove("${RedisKeys.newPack}:${id.id}")
}