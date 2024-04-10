package org.icpclive.tickermerger

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.GlobalScope
import org.icpclive.tickermerger.plugins.*

fun main() {
    embeddedServer(Netty, port = 8085, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    launchMerge()
    setupKtorPlugins()
    configureRouting()
}
