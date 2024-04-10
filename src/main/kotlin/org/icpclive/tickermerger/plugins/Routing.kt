package org.icpclive.tickermerger.plugins

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.icpclive.tickermerger.data.DataBus
import org.icpclive.util.sendJsonFlow

inline fun <reified T: Any> Route.flowEndpoint(name: String, crossinline dataProvider: suspend () -> Flow<T>) {
    webSocket(name) { sendJsonFlow(dataProvider()) }
    get(name) { call.respond(dataProvider().first()) }
}


fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello World!")
        }

        for ((contest, manager) in DataBus.managers){
            route("/api/${contest}") {
                flowEndpoint("/ticker") { manager.eventsFlow }
            }
        }
    }
}
