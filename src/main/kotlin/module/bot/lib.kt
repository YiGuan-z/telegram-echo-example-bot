package module.bot

/**
 *
 * @author caseycheng
 * @date 2023/10/15-21:45
 * @doc
 **/
/**
 * 解析参数，将参数解析为Map
 */
fun List<String>.resolveArgs(): Map<String, String> {
    val result: MutableMap<String, String> = mutableMapOf()
    var key = ""
    this.forEachIndexed { index, arg ->
        if (index % 2 == 0) {
            key = arg
        } else {
            result[key] = arg
        }
    }
    return result
}
