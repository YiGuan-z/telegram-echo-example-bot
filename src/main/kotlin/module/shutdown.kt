package module

import application.createAppPlugin

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
    private val hooks: MutableList<Runnable> = mutableListOf()

    fun addShutDownHook(runable: Runnable) {
        this.hooks.add(runable)
    }

    internal fun plan() {
        val thread = Thread({
            for (hook in hooks) {
                hook.run()
            }
        }, "shutdown")

        Runtime.getRuntime().addShutdownHook(thread)
    }
}