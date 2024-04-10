package org.icpclive.api

import kotlin.random.Random
import kotlin.random.nextUInt

fun generateId(widgetPrefix: String): String = "$widgetPrefix-${Random.nextUInt()}"
