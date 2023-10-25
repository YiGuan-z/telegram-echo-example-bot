package github.cheng.module.bot.modal

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

data class StickerCollectPack(
    val start :Long = -1,
    val files:Set<String> = emptySet(),
    val srcImg:Set<String> = emptySet(),
    val destImg:Set<String> = emptySet(),
    val isLocked:Boolean = false
)

fun StickerCollectPack.startTime():LocalDateTime?{
    return if (start == -1L){
        null
    }else{
        LocalDateTime
            .ofInstant(Instant.ofEpochSecond(start), ZoneOffset.ofHours(8))
    }
}