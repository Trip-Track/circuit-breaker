package swa

import io.ktor.server.application.*
import swa.UrlResolver.discoverService
import swa.circuit_breaker.CircuitBreaker

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    //val tripPlannerCB = CircuitBreaker {discoverService(environment.config.property("services.trip_planner").getString())}

    //val cityInfoCB = CircuitBreaker {discoverService(environment.config.property("services.city_info").getString())}

    val tripPlannerCB = CircuitBreaker { -> "http://0.0.0.0:8089"}

    val cityInfoCB = CircuitBreaker {discoverService(environment.config.property("services.city_info").getString())}


    configureSerialization()
    configureRouting(tripPlannerCB, cityInfoCB)
}
