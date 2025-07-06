package swa

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.path
import io.ktor.server.response.*
import io.ktor.server.routing.*
import swa.circuit_breaker.CircuitBreaker
import swa.circuit_breaker.CircuitBreakerReject


fun Application.configureRouting() {

    val circuitBreaker = CircuitBreaker(
        baseUrl = System.getenv("WEATHER_URL") ?: "http://weather:8000"
    )

    routing {
        get("/weather") {

            try {
                circuitBreaker.routeRequest(call.request.path(), call.request.queryParameters)
                call.respond("")
            } catch (_: CircuitBreakerReject) {
                call.respond(HttpStatusCode.ServiceUnavailable,
                    "Weather temporarily unavailable")
            } catch (ex: Exception) {
                println(ex.message)
                println(ex)
                call.respond(HttpStatusCode.BadGateway)
            }
        }
    }
}
