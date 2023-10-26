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
//在这里对不同的args做缓存，缓存那些已经生成出来了的实例
fun commandArgs(args: Array<String>): CommandArgs = CommandArgsImpl(args)


interface CommandArgs : ReadOnlyProperty<Any?, String>

internal class CommandArgsImpl(
    private val args: Array<String>
) : CommandArgs {
    //只有在getValue的时候触发一次。
    override fun getValue(thisRef: Any?, property: KProperty<*>): String {
        val awaitResolveMark = property.name
        val pairs = args.mapNotNull { it.splitPair('=') }.toMap()
        return pairs[awaitResolveMark] ?: throw IllegalArgumentException("$awaitResolveMark is not a valid argument")
    }
}

