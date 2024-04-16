package org.icpclive.tickermerger

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.icpclive.api.*
import org.icpclive.data.TickerManager
import org.icpclive.util.getLogger
import kotlin.time.Duration

private sealed class TickerProcessorEvent {
    data class ContestEvent(val contest: String, val event: TickerEvent) : TickerProcessorEvent()
    data class SwitchEvent(val messageId: String?) : TickerProcessorEvent()
}


class TickerEventsProcessor(private val managers: Map<String, TickerManager>, private val tickerPart: TickerPart) {
    private var messages = listOf<Pair<String, TickerMessage>>()
    private val events = MutableSharedFlow<TickerProcessorEvent>()
    private var currentMessage = 0
    private val currentShownMessagesIds = mutableMapOf<String, String?>()
    private var switchJob: Job? = null

    suspend fun processEvent(contest: String, event: TickerEvent) {
        check(contest in managers) { "Unexpected contest $contest" }
        logger.info("Process new event from $contest: $event")
        events.emit(TickerProcessorEvent.ContestEvent(contest, event))
    }

    suspend fun run(coroutineScope: CoroutineScope) {
        coroutineScope.scheduleSwitch(1000, null)

        events.collect { processorEvent ->
            when (processorEvent) {
                is TickerProcessorEvent.ContestEvent -> {
                    val (contest, event) = processorEvent
                    when (event) {
                        is TickerSnapshotEvent -> {
                            logger.info("TickerSnapshotEvent $event")
                            val addedMessages = event.messages.filter { it.part == tickerPart }
                            val newCurrentMessage = messages
                                .filterIndexed { index, (mContest, _) -> mContest != contest && index < currentMessage }
                                .count()

                            messages = messages.filter { it.first != contest } + addedMessages.map { contest to it }
                            if (messages.isEmpty()) {
                                showEmpty(coroutineScope)
                                return@collect
                            }
                            currentMessage = newCurrentMessage % messages.size
                            val (newMessageContest, newMessage) = messages[currentMessage]
                            showMessage(coroutineScope, newMessageContest, newMessage)
                        }

                        is AddMessageTickerEvent -> {
                            logger.info("AddMessageTickerEvent ${event.message}")
                            if (event.message.part != tickerPart) {
                                return@collect
                            }
                            messages += contest to event.message
                        }

                        is RemoveMessageTickerEvent -> {
                            logger.info("RemoveMessageTickerEvent ${event.messageId}")
                            if (!messages.any { it.second.id == event.messageId }) {
                                return@collect
                            }
                            val messageIndex = messages.indexOfFirst { it.second.id == event.messageId }
                            messages = messages.filterIndexed { index, _ -> index != messageIndex }
                            if (messages.isEmpty()) {
                                showEmpty(coroutineScope)
                                return@collect
                            }
                            if (messageIndex < currentMessage) {
                                currentMessage--
                                return@collect
                            } else if (messageIndex == currentMessage) {
                                currentMessage %= messages.size
                                val (newMessageContest, newMessage) = messages[currentMessage]
                                showMessage(coroutineScope, newMessageContest, newMessage)
                            }
                        }
                    }
                }

                is TickerProcessorEvent.SwitchEvent -> {
                    logger.info("${tickerPart}: Switch event ${messages.size}")

                    if (messages.isEmpty()) {
                        showEmpty(coroutineScope)
                        return@collect
                    }

                    val newCurrentMessage = (currentMessage + 1) % messages.size
                    val (newMessageContest, newMessage) = messages[newCurrentMessage]
                    logger.info("SwitchEvent from ${messages[currentMessage % messages.size].second} ($currentMessage) to $newMessage")

                    showMessage(coroutineScope, newMessageContest, newMessage)
                    currentMessage = newCurrentMessage
                }
            }
        }
    }

    private suspend fun showEmpty(coroutineScope: CoroutineScope) {
        switchJob?.cancel()
        for ((key, manager) in managers) {
            currentShownMessagesIds[key]?.let { manager.remove(it) }
            currentShownMessagesIds[key] = null
        }
        switchJob = coroutineScope.scheduleSwitch(250, null)
    }

    private suspend fun showMessage(coroutineScope: CoroutineScope, contest: String, newMessage: TickerMessage) {
//        if (currentShownMessagesIds[contest] == newMessage.id) {
//            switchJob?.cancel()
//            switchJob = coroutineScope.scheduleSwitch(newMessage.periodMs, newMessage.id)
//            return
//        }
        switchJob?.cancel()
        val newMessagesIds = mutableMapOf<String, String>()
        for ((key, manager) in managers) {
            if (key == contest) {
                logger.info("add to manager $key message $newMessage (${newMessage.id})")

                manager.add(newMessage)
                newMessagesIds[key] = newMessage.id
            } else {
                logger.info("add to manager $key empty message $newMessage (${newMessage.id})")
                val settings = emptyMessage(newMessage)
                newMessagesIds[key] = settings.id
                manager.add(settings)
            }
        }

        for ((key, manager) in managers) {
            logger.info("remove from manager $key message ${currentShownMessagesIds[key]}")
            currentShownMessagesIds[key]?.takeIf { it != newMessagesIds[key] }?.let { manager.remove(it) }
            currentShownMessagesIds[key] = newMessagesIds[key]
            logger.info("remove from manager $key message ${currentShownMessagesIds[key]}")
        }
        switchJob = coroutineScope.scheduleSwitch(newMessage.periodMs, newMessage.id)
    }

    private fun CoroutineScope.scheduleSwitch(delay: Long, messageId: String?) = launch {
        delay(delay)

        events.emit(TickerProcessorEvent.SwitchEvent(messageId))
    }

    companion object {
        private fun emptyMessage(message: TickerMessage) = EmptyTickerSettings(
            message.part,
            message.periodMs
        ).toMessage()

        private val logger = getLogger(TickerProcessorEvent::class)
    }
}
