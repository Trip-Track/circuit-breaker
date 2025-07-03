package swa

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
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
            val city = call.request.queryParameters["city"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "city required")

            try {
                circuitBreaker.routeRequest(city)
                call.respond("")
            } catch (_: CircuitBreakerReject) {
                call.respond(HttpStatusCode.ServiceUnavailable,
                    "Weather temporarily unavailable (circuit open)")
            } catch (ex: Exception) {
                call.respond(HttpStatusCode.BadGateway, ex.localizedMessage)
            }
        }
    }
}
