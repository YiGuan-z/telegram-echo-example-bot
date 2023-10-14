package module.request

import application.createAppPlugin
import io.ktor.client.*
import io.ktor.client.engine.cio.*

/**
 *
 * @author caseycheng
 * @date 2023/10/14-13:53
 * @doc
 **/
val httpClient = createAppPlugin("ktorClient", ::Any) {
    HttpClient(CIO)
}
