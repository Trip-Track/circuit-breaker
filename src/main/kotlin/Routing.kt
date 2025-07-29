package swa

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.path
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.jvm.javaio.copyTo
import swa.circuit_breaker.CircuitBreaker
import swa.circuit_breaker.CircuitBreakerReject



fun Application.configureRouting(
    tripPlannerCB: CircuitBreaker,
    cityInfoCB: CircuitBreaker
) {

    routing {

        route("/trip") {
            get {
                try {
                    val rsp = tripPlannerCB.routeRequest(
                        call.request.path(),
                        call.request.queryParameters
                    )
                    call.respondProxy(rsp)

                } catch (_: CircuitBreakerReject) {
                    call.respond(HttpStatusCode.ServiceUnavailable,
                        "Path temporarily unavailable")
                } catch (ex: Exception) {
                    log.error("Exception while routing request to ${call.request.path()}")
                    ex.printStackTrace()
                    call.respond(HttpStatusCode.BadGateway)
                }
            }
        }

        get("/map") {
            try {
                val rsp = tripPlannerCB.routeRequest(
                    call.request.path(),
                    call.request.queryParameters
                )
                call.respondProxy(rsp)

            } catch (_: CircuitBreakerReject) {
                call.respond(HttpStatusCode.ServiceUnavailable,
                    "Map temporarily unavailable")
            } catch (ex: Exception) {
                log.error("Exception while routing request to ${call.request.path()}")
                ex.printStackTrace()
                call.respond(HttpStatusCode.BadGateway)
            }
        }

        route("/city/{city}/{endpoint}") {
            get {
                try {
                    val rsp = cityInfoCB.routeRequest(
                        call.request.path(),
                        call.request.queryParameters
                    )
                    call.respondProxy(rsp)
                } catch (_: CircuitBreakerReject) {
                    call.respond(HttpStatusCode.ServiceUnavailable,
                        "City-info temporarily unavailable")
                } catch (ex: Exception) {
                    log.error("Exception while routing request to ${call.request.path()}")
                    ex.printStackTrace()
                    call.respond(HttpStatusCode.BadGateway)
                }
            }
        }
    }
}

suspend fun ApplicationCall.respondProxy(rsp: HttpResponse) {
    response.status(rsp.status)

    rsp.headers[HttpHeaders.ContentType]?.let { ct ->
        response.headers.append(HttpHeaders.ContentType, ct)
    }

    respondOutputStream {
        rsp.bodyAsChannel().copyTo(this)
    }
}
