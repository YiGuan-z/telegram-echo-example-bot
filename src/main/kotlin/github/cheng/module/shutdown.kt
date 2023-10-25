package github.cheng.module

import github.cheng.application.createAppPlugin

/**
 *
 * @author caseycheng
 * @date 2023/10/18-14:50
 * @doc
 **/
internal val shutdown = createAppPlugin("shatdown", ::Any) {
    ShutDown
}

object ShutDown {
    private val hooks: MutableList<Pair<String?, Runnable>> = mutableListOf()
    private val logger = logger()

    fun addShutDownHook(name: String? = null, runable: Runnable) {
        this.hooks.add(name to runable)
    }

    internal fun plan() {
        val thread = Thread({
            for ((name, hook) in hooks) {
                hook.run()
                name?.let {
                    logger.info("shutdown hook: $it")
                }
            }
        }, "shutdown")

        Runtime.getRuntime().addShutdownHook(thread)
    }
}