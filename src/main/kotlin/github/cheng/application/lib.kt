package github.cheng.application

/**
 *
 * @author caseycheng
 * @date 2023/10/29-11:00
 * @doc
 **/
@Suppress("unchecked_cast")
fun <R> Iterable<*>.filterIsAssignableFrom(cls: Class<R>): List<R> =
    filterNotNull().filter { it::class.java.isAssignableFrom(cls) }.toList() as List<R>

internal fun ByteArray.readToString() = String(this)
