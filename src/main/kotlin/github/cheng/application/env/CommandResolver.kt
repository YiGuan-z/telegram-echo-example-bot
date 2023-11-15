package github.cheng.application.env

import github.cheng.application.splitPair
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 *
 * @author caseycheng
 * @date 2023/10/21-02:22
 * @doc
 **/
@JvmField
val argsCache: LRUCache<Int, Map<String, String>> = LRUCache()

fun commandArgs(args: Array<String>): CommandArgs = CommandArgsImpl(args)

fun commandArgs(args: Array<String>, key: String): CommandArgs = CommandArgsImplForKey(args, key)

interface CommandArgs : ReadOnlyProperty<Any?, String> {
    fun getCache(args: Array<String>): Map<String, String> =
        argsCache.getOrPut(args.contentDeepHashCode()) { args.mapNotNull { it.splitPair('=') }.toMap() }
}

internal class CommandArgsImpl(
    private val args: Array<String>,
) : CommandArgs {
    // 只有在getValue的时候触发一次。
    override fun getValue(
        thisRef: Any?,
        property: KProperty<*>,
    ): String {
        val cache = getCache(args)
        val awaitResolveMark = property.name
        return cache[awaitResolveMark] ?: throw IllegalArgumentException("$awaitResolveMark is not a valid argument")
    }
}

internal class CommandArgsImplForKey(
    private val args: Array<String>,
    private val key: String
) : CommandArgs {
    override fun getValue(
        thisRef: Any?,
        property: KProperty<*>
    ): String {
        val cache = getCache(args)
        return cache[key] ?: throw IllegalArgumentException("$key is not a valid argument")
    }
}

class LRUCache<K, V>(
    @Suppress("MemberVisibilityCanBePrivate")
    var maxEntries: Int = 100
) : LinkedHashMap<K, V>() {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>): Boolean {
        return size > maxEntries
    }
}

fun <K : Any, V : Any> LRUCache<K, V>.setMapEntries(max: Int) {
    maxEntries = max
}