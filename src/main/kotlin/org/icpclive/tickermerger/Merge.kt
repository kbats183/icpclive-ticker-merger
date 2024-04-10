package org.icpclive.tickermerger

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.icpclive.api.TickerEvent
import org.icpclive.api.TickerPart
import org.icpclive.tickermerger.data.DataBus
import org.icpclive.util.getLogger
import kotlin.time.Duration.Companion.seconds

val client = HttpClient(CIO) {
    install(WebSockets) {
        pingInterval = 20_000
        contentConverter = KotlinxWebsocketSerializationConverter(Json)
    }
}

fun CoroutineScope.launchMerge() {
//    val processors = listOf(TickerPart.LONG).map { TickerEventsProcessor(DataBus.managers, it) }
    val processors = TickerPart.entries.map { TickerEventsProcessor(DataBus.managers, it) }
    processors.forEach {
        launch { it.run(this) }
    }

    DataBus.sources.forEach {(contest, url) ->
        launch {
            runWebsocketListening(url, processors, contest)
        }
    }
}

private suspend fun runWebsocketListening(
    url: String,
    processors: List<TickerEventsProcessor>,
    contest: String
) {
    while (true) {
        try {
            client.webSocket(url) {
                while (true) {
                    val event = receiveDeserialized<TickerEvent>()
                    processors.forEach { it.processEvent(contest, event) }
                }
            }
        } catch (e: RuntimeException) {
            logger.warn("WebSocket connection to $url failed", e)
        }
        delay(1.seconds)
    }
}

val logger = getLogger(HttpClient::class)

