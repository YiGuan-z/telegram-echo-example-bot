package github.cheng

object TelegramResources {
    var imageStorage: String by setOnce()
    var defaultLang: String by setOnce()
    var adminName: String by setOnce()
    var maxImages: Int by setOnce()
    var stickerSources: List<String> by setOnce()
}
