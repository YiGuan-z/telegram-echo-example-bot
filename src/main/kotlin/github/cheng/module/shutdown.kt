package github.cheng.module

import github.cheng.application.createAppPlugin
import kotlin.concurrent.thread

/**
 *
 * @author caseycheng
 * @date 2023/10/18-14:50
 * @doc
 **/
internal val shutdown =
    createAppPlugin("shutdown", ::Any) {
        ShutDown
    }

object ShutDown {
    private val hooks: MutableList<Pair<String?, Runnable>> = mutableListOf()
    private val logger = logger()

    fun addShutDownHook(
        name: String? = null,
        runable: Runnable,
    ) {
        this.hooks.add(name to runable)
    }

    internal fun plan() {
        val thread = thread(start = false, name = "shutdown") {
            for ((name, hook) in hooks) {
                hook.run()
                name?.let {
                    logger.info("shutdown hook: $it")
                }
            }
        }
        Runtime.getRuntime().addShutdownHook(thread)
    }
}
