package application

import module.bot.currentPath
import module.jackson
import module.thisLogger
import org.slf4j.Logger
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipInputStream
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.isDirectory

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
    fun get(language: Language): LanguagePack
    fun keys(): Set<String>
    fun reload()
}

interface LanguagePack {
    //通过语言包的key获取到对应模块的语言包
    fun pack(path: String): LanguagePack

    fun getString(key: String): String

    //被format后的字符串
    fun getString(key: String, mark: String, arg: String): String
    fun getString(key: String, vararg props: Pair<String, String>): String
}

internal class I18nPacksImpl(private val function: GenerateLanguages) : I18nPacks {
    private var root: Map<Language, Any> = function()
    private val logger: Logger = thisLogger<I18nPacks>()
    override fun get(language: Language): LanguagePack {
        val row = root[language] ?: throw I18nBuildException("$language is not exist")

        @Suppress("unchecked_cast")
        val lang = row as? Map<String, Any> ?: throw I18nBuildException("$language is not a map")
        if (lang.isEmpty()) throw I18nBuildException("$language is empty")
        return LanguagePackImpl(lang, "", language.lang)
    }

    override fun keys(): Set<String> {
        return root.keys.map { it.lang }.toSet()
    }

    override fun reload() {
        root = function()
    }

    /**
     * 这里的检查函数需要进行深入检查
     * 检查每一条字符串的对应标记
     * 检查对象中的字段个数
     * 以第一个为准，其它的如果发现与第一个不一致，那么就抛出异常并终止运行
     * 检查字段和它对应的值中的mark标记是否全部一致
     */
    private fun checkLang() {
        //获取结构
        fun getFirstStruct(root: Map<String, Any>): Map<String, Any> {
            val structure: MutableMap<String, Any> = mutableMapOf()
            root.forEach { (key, value) ->
                if (value is String) {
                    structure[key] = value.getMarks()
                } else {
                    @Suppress("unchecked_cast")
                    structure[key] = getFirstStruct(value as Map<String, Any>)
                }
            }
            return structure
        }

        @Suppress("unchecked_cast")
        val structure = getFirstStruct(root.values.first() as Map<String, Any>)
        logger.trace("[i18n] get first structure: \n {}", structure)
        //验证后续结构
        @Suppress("unchecked_cast")
        fun checkStruct(node: Map<String, Any>, struct: Map<String, Any>) {
            node.forEach { (key, data) ->
                if (data is String) {
                    val marks = data.getMarks()
                    if (marks != struct[key]) {
                        logger.error("[i18n] check fail key $key expected mark is:\n ${struct[key]}\n but get it is:\n $marks")
                        throw I18nCheckException("[i18n] check fail key $key expected mark is:\n ${struct[key]}\n but get it is:\n $marks")
                    }
                } else {
                    data as Map<String, Any>
                    val children =
                        struct[key] as? Map<*, *> ?: throw I18nCheckException("[i18n] check lang failed: $key")
                    checkStruct(data, children as Map<String, Any>)
                }
            }
        }
        root.forEach { (language, node) ->
            @Suppress("unchecked_cast")
            node as? Map<String, Any>
                ?: throw I18nCheckException("${language.lang} should be a map, not be a ${node::class.simpleName}")
            checkStruct(node, structure)
        }
    }
}

private val regex = Regex("\\{(.*?)\\}")

//被 {} 包裹住的文字就是mark，获取这些标记，并返回出去。
private fun String.getMarks(): List<String> {
    val matcher = regex.findAll(this)
    val iterator = matcher.iterator()
    if (!iterator.hasNext()) {
        return emptyList()
    }
    val list = mutableListOf<String>()
    iterator.forEach {
        //its value is {lang}, so used drop function
        list.add(it.value.dropLast(1).drop(1))
    }
    return list
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
        val parts = combine(this.path, key).split('.')
        val fold = parts.dropLast(1).fold(root) { node, pat ->
            @Suppress("unchecked_cast")
            node[pat] as? Map<String, Any> ?: throw I18nBuildException("$pat is not a map")
        }
        return fold[parts.last()] as String
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
class I18nCheckException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

data class Language(val lang: String = "") {
    companion object {
        @JvmStatic
        fun of(lang: String) = Language(lang)
    }
}

fun Language.isBlank() = lang.isBlank()
// zh_CN [{message:"消息"},messages:[{message1:"提示消息",message2:"重要消息"}]]
typealias GenerateLanguages = () -> Map<Language, Map<String, Any>>

class I18nPacksBuildScope {
    // Map value should be a map or string, so this is Any.
    // added TypePair using represents two types
    internal val languages: MutableMap<Language, Map<String, Any>> = mutableMapOf()
    var generateLanguages: GenerateLanguages? = null

    fun build(): I18nPacks {
        val function = requireNotNull(generateLanguages) { "need get a generateLanguages function" }
        @Suppress("INVISIBLE_MEMBER")
        return I18nPacksImpl(function).apply { checkLang() }
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

fun Application.getJsonFiles(): Map<Language, Map<String, Any>> {
    val mapper = installAndInstance(jackson)
//    val url = Thread.currentThread().contextClassLoader.getResource("i18n")?.toURI()
//    logger.info("[i18n] geturl:{}", url)
//    val files = File(url ?: throw RuntimeException("i18n folder not found"))
    val tempDir = Path(currentPath() + "/temp")
    val files: File =
        if (tempDir.isDirectory()) {
            //存在就获取文件
            tempDir.toFile().listFiles()!![0]
        } else {
            logger.info("entry")
            //不存在就解压文件
            getI18nFile()
        }
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
        Language.of(language) to data
    }
    files.deleteOnExit()
    return languages
}

private fun Application.getI18nFile(): File {
    val i18nInputStream = Thread.currentThread().contextClassLoader.getResourceAsStream("i18n.zip")
        ?: throw RuntimeException("need i18n files")
    val zipInputStream = ZipInputStream(i18nInputStream)
    val tempDirectory = createTempDirectory(createTempDir("/temp"), "i18n_temp")
    zipInputStream.use { zip ->
        generateSequence { zip.nextEntry }.forEach { entry ->
            val outputFile = tempDirectory.resolve(entry.name)
            if (entry.isDirectory) {
                Files.createDirectories(outputFile)
            } else {
                Files.createDirectories(outputFile.parent)
                Files.newOutputStream(outputFile).use { out ->
                    zip.copyTo(out)
                }
            }
        }
    }
    return tempDirectory.toFile()
}

fun createTempDir(name: String): Path {
    val path = Path(currentPath() + name).createDirectories()
    return path
}