package application.env

import module.bot.toMap

/**
 *
 * @author caseycheng
 * @date 2023/10/21-02:22
 * @doc
 **/
class CommandResolver(args: List<String>) {
    val map = args.toMap()

    constructor() : this(emptyList())

    fun resolveNullable(mark: String): String? {
        return map[mark]
    }

    fun resolve(mark: String): String =
        resolveNullable(mark) ?: throw IllegalArgumentException("$mark is not a valid argument")

//    fun resolveOptions()

}

data class OptionProvide(
    val mark: String,
    val defaultValue: String,
    val require: Boolean,
    val onGet: (String) -> Unit
) {
    constructor(mark: String, defaultValue: String, onGet: (String) -> Unit) : this(mark, defaultValue, true, onGet)
}