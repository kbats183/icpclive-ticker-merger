package org.icpclive.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

interface ObjectSettings

@Serializable
sealed class TickerMessageSettings : ObjectSettings {
    abstract val part: TickerPart
    abstract val periodMs: Long
    abstract fun toMessage(): TickerMessage
}

@Serializable
enum class TickerPart {
    @SerialName("short")
    SHORT,

    @SerialName("long")
    LONG;
}

@Serializable
@SerialName("text")
data class TextTickerSettings(
    override val part: TickerPart, override val periodMs: Long, val text: String
) : TickerMessageSettings() {
    override fun toMessage() = TextTickerMessage(this)
}

@Serializable
@SerialName("image")
data class ImageTickerSettings(
    override val part: TickerPart, override val periodMs: Long, val path: String
) : TickerMessageSettings() {
    override fun toMessage() = ImageTickerMessage(this)
}

@Serializable
@SerialName("clock")
data class ClockTickerSettings(
    override val part: TickerPart,
    override val periodMs: Long,
    val timeZone: String? = null
) : TickerMessageSettings() {
    override fun toMessage(): ClockTickerMessage {
        if (timeZone != null && timeZone.isEmpty()) {
            return ClockTickerMessage(ClockTickerSettings(part, periodMs, null))
        }
        return ClockTickerMessage(this)
    }
}

@Serializable
@SerialName("scoreboard")
data class ScoreboardTickerSettings(
    override val part: TickerPart, override val periodMs: Long, val from: Int, val to: Int
) : TickerMessageSettings() {
    override fun toMessage() = ScoreboardTickerMessage(this)
}

@Serializable
@SerialName("empty")
data class EmptyTickerSettings(
    override val part: TickerPart, override val periodMs: Long
) : TickerMessageSettings() {
    override fun toMessage() = EmptyTickerMessage(this)
}
