import java.nio.ByteBuffer
import kotlin.properties.Delegates
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 *
 * @author caseycheng
 * @date 2023/10/2-13:42
 * @doc
 **/
fun envOfNullable(key: String): String? = System.getenv(key)

fun env(key: String): String = envOfNullable(key) ?: throw IllegalArgumentException("env $key not found")

inline fun <reified T : Any> Delegates.setOnce(): ReadWriteProperty<Any?, T> = SetOnce()

inline fun <reified T : Any> setOnce(): ReadWriteProperty<Any?, T> = SetOnce()

/**
 * 只能够写入一次属性
 */
class SetOnce<T : Any> : ReadWriteProperty<Any?, T> {
    private var value: T? = null
    private var isSet = false
    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return value ?: throw IllegalStateException("Property $property has not been initialized")
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        if (isSet) throw IllegalStateException("Property $property has already been initialized")
        this.value = value
        isSet = true
    }
}

fun ByteBuffer.getByte(): ByteArray {
    val byteArray = ByteArray(this.remaining())
    get(byteArray)
    return byteArray
}