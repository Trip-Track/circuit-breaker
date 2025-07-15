package swa

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.path
import io.ktor.server.response.*
import io.ktor.server.routing.*
import swa.circuit_breaker.CircuitBreaker
import swa.circuit_breaker.CircuitBreakerReject



fun Application.configureRouting() {

    val tripPlannerCB = CircuitBreaker(
        baseUrl = System.getenv("TRIP_PLANNER_URL") ?: "http://trip-planner:8000"
    )
    val cityInfoCB = CircuitBreaker(
        baseUrl = System.getenv("CITY_INFO_URL") ?: "http://city-info:8000"
    )

    routing {

        route("/trip") {
            get {
                try {
                    tripPlannerCB.routeRequest(call.request.path(), call.request.queryParameters)
                    call.respond("")                    // body already streamed back
                } catch (_: CircuitBreakerReject) {
                    call.respond(HttpStatusCode.ServiceUnavailable,
                        "Trip-planner temporarily unavailable")
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    call.respond(HttpStatusCode.BadGateway)
                }
            }
        }

        get("/map") {
            try {
                tripPlannerCB.routeRequest(call.request.path(), call.request.queryParameters)
                call.respond("")
            } catch (_: CircuitBreakerReject) {
                call.respond(HttpStatusCode.ServiceUnavailable,
                    "Trip-planner temporarily unavailable")
            } catch (ex: Exception) {
                ex.printStackTrace()
                call.respond(HttpStatusCode.BadGateway)
            }
        }

        route("/city/{city}/{endpoint}") {
            get {
                try {
                    cityInfoCB.routeRequest(call.request.path(), call.request.queryParameters)
                    call.respond("")
                } catch (_: CircuitBreakerReject) {
                    call.respond(HttpStatusCode.ServiceUnavailable,
                        "City-info temporarily unavailable")
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    call.respond(HttpStatusCode.BadGateway)
                }
            }
        }
    }
}
