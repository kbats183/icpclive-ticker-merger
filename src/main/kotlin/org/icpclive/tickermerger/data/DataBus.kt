package org.icpclive.tickermerger.data

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.*
import org.icpclive.api.*
import org.icpclive.data.TickerManager

/**
 * Everything published here should be immutable, to allow secure work from many threads
 */
object DataBus {
    val sources = mapOf(
        "46th" to "ws://localhost:8081/api/overlay/ticker",
        "47th" to "ws://localhost:8082/api/overlay/ticker"
    )
    val managers = sources.mapValues { TickerManager() }
}
