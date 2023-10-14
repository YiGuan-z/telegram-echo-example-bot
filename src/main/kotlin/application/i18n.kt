package application

import module.jackson
import java.io.File

/**
 *
 * @author caseycheng
 * @date 2023/10/12-18:14
 * @doc
 **/
val i18n = createAppPlugin("i18n", ::I18nPacksBuildScope) {
    it.build()
}

/**
 * zh_CN.json
 * en_US.json
 */
interface I18nPacks {
    // language = zh_CN
    // language = en_US
    fun get(language: String): LanguagePack
    fun keys(): Set<String>
}

interface LanguagePack {
    //通过语言包的key获取到对应模块的语言包
    fun pack(path: String): LanguagePack

    fun getString(key: String): String

    //被format后的字符串
    fun getString(key: String, mark: String, arg: String): String
    fun getString(key: String, vararg props: Pair<String, String>): String
}

internal class I18nPacksImpl(private val root: Map<String, Any>) : I18nPacks {
    override fun get(language: String): LanguagePack {
        val row = root[language] ?: throw I18nBuildException("$language is not exist")

        @Suppress("unchecked_cast")
        val lang = row as? Map<String, Any> ?: throw I18nBuildException("$language is not a map")
        return LanguagePackImpl(lang, "", language)
    }

    override fun keys(): Set<String> {
        return root.keys
    }
}

internal class LanguagePackImpl(
    private val root: Map<String, Any>,
    private val path: String,
    @Suppress("MemberVisibilityCanBePrivate")
    val lang: String
) : LanguagePack {
    val isRoot = path.isEmpty()

    override fun pack(path: String): LanguagePack {
        return LanguagePackImpl(root, combine(this.path, path), lang)
    }

    override fun getString(key: String): String {
        val parts = this.path.split('.')
        val fold = parts.fold(root) { node, pat ->
            @Suppress("unchecked_cast")
            node[pat] as? Map<String, Any> ?: throw I18nBuildException("$pat is not a map")
        }
        return fold[key] as String
    }

    override fun getString(key: String, mark: String, arg: String): String {
        return getString(key).replace("{${mark}}", arg)
    }

    override fun getString(key: String, vararg props: Pair<String, String>): String {
        var text = getString(key)
        for ((mark, arg) in props) {
            text = text.replace("{${mark}}", arg)
        }
        return text
    }
}

class I18nBuildException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class I18nPacksBuildScope {
    // Map value should be a map or string, so this is Any.
    // added TypePair using represents two types
    internal val languages: MutableMap<String, Map<String, Any>> = mutableMapOf()

    // zh_CN [{message:"消息"},messages:[{message1:"提示消息",message2:"重要消息"}]]
    fun addPack(language: String, buildBlock: () -> Map<String, Any>) {
        if (language.isBlank()) {
            throw I18nBuildException("language can not be blank")
        }

        if (languages.containsKey(language)) {
            throw I18nBuildException("language $language already exists")
        }

        val map = buildBlock()
        languages[language] = map
    }

    fun build(): I18nPacks {
        return I18nPacksImpl(languages).apply { checkLang() }
    }

    private fun checkLang() {
        languages.forEach { (lang, data) ->
            if (lang.isBlank()) {
                throw I18nBuildException("language can not be blank")
            }
            data.forEach {
                if (it.key.isBlank()) {
                    throw I18nBuildException("key can not be blank")
                }
            }
        }
    }

}

@Suppress("UNCHECKED_CAST")
internal class TypePair<E1, E2>(private val element: Any) {
    fun getType1OrNull(): E1? {
        return element as? E1
    }

    fun getType2OrNull(): E2? {
        return element as? E2
    }
}

internal inline fun <reified E1, reified E2> TypePair<E1, E2>.getType1() =
    getType1OrNull() ?: throw TypeCastException("Can not cast to ${E1::class.java}")

internal inline fun <reified E1, reified E2> TypePair<E1, E2>.getType2() =
    getType2OrNull() ?: throw TypeCastException("Can not cast to ${E2::class.java}")


private fun combine(root: String, relative: String): String = if (root.isEmpty()) {
    relative
} else {
    "$root.$relative"
}

fun Application.getJsonFiles(): Map<String, Map<String, Any>> {
    val mapper = installAndInstance(jackson)
    val url = Thread.currentThread().contextClassLoader.getResource("i18n")?.toURI()
    val files = File(url ?: throw RuntimeException("i18n folder not found"))
    if (!files.exists()) {
        throw RuntimeException("i18n folder not found")
    }
    val jsons = files.listFiles() ?: throw RuntimeException("need i18n files")
    val languages = jsons.associate { file ->
        logger.info("reading {}", file.name)
        val fileName = file.name
        val language = fileName.dropLast(fileName.indexOf(".json"))
        val reader = file.bufferedReader()
        val map = mapper.readValue(reader.readText(), Map::class.java)

        @Suppress("unchecked_cast")
        val data = map as? Map<String, Any> ?: throw IllegalArgumentException("json file is not valid")
        language to data
    }
    return languages
}