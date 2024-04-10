package org.icpclive.data

import kotlinx.coroutines.flow.Flow
import org.icpclive.api.*
import org.icpclive.tickermerger.data.DataBus
import org.icpclive.util.completeOrThrow

class TickerManager : ManagerWithEvents<TickerMessage, TickerEvent>() {
    override fun createAddEvent(item: TickerMessage) = AddMessageTickerEvent(item)
    override fun createRemoveEvent(id: String) = RemoveMessageTickerEvent(id)
    override fun createSnapshotEvent(items: List<TickerMessage>) = TickerSnapshotEvent(items)

    val eventsFlow = flow
}
