@file:Suppress("unused")

package org.icpclive.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class Event

@Serializable
sealed class TickerEvent : Event()

@Serializable
@SerialName("AddMessage")
class AddMessageTickerEvent(val message: TickerMessage) : TickerEvent()

@Serializable
@SerialName("RemoveMessage")
class RemoveMessageTickerEvent(val messageId: String) : TickerEvent()

@Serializable
@SerialName("TickerSnapshot")
class TickerSnapshotEvent(val messages: List<TickerMessage>) : TickerEvent()
