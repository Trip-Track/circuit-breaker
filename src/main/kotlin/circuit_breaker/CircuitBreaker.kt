package swa.circuit_breaker

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.java.Java
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.timeout
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.json.Json
import java.time.Instant

class CircuitBreakerReject: RuntimeException("call rejected by Circuit Breaker")


sealed interface State {
    suspend fun processRequest(city: String)

    suspend fun sendReq(httpClient: HttpClient, baseUrl: String, city: String): HttpResponse {
        return httpClient.get("$baseUrl/weather") {
            url { parameters.append("city", city) }
            timeout { requestTimeoutMillis = 3_000 }
        }
    }

    class Closed(private val cb: CircuitBreaker): State {
        var failureCountExpiration: Instant = Instant.now().plusMillis(cb.resetTimeoutMs)
        init {
            cb.failureCount = 0
        }

        override suspend fun processRequest(city: String) {
            val rsp = sendReq(cb.http, cb.baseUrl, city)

            val now = Instant.now()
            if ( now.isAfter(failureCountExpiration)){
                cb.failureCount = 0
                failureCountExpiration = now.plusMillis(cb.resetTimeoutMs)
            }

            if (rsp.status.isSuccess()) return rsp.body()

            cb.failureCount++
            if (cb.failureCount > cb.failThreshold) cb.changeState(Open(cb))
            throw CircuitBreakerReject()
        }
    }


    class Open(private val cb: CircuitBreaker): State {
        val timerExpiration: Instant = Instant.now().plusMillis(cb.resetTimeoutMs)

        override suspend fun processRequest(city: String) {
            if ( Instant.now().isAfter(timerExpiration)){
                val rsp = sendReq(cb.http, cb.baseUrl, city)
                if (rsp.status.isSuccess()) {
                    cb.changeState(HalfOpen(cb))
                    return rsp.body()
                }
            }
            throw CircuitBreakerReject()

        }
    }


    class HalfOpen(private val cb: CircuitBreaker): State {
        init {
            cb.successCount = 0
        }
        override suspend fun processRequest(city: String) {
            val rsp = sendReq(cb.http, cb.baseUrl, city)
            if (rsp.status.isSuccess()) {
                if(++cb.successCount >= cb.halfOpenSuccThreshold) cb.changeState(Closed(cb))
                return rsp.body()
            }
            cb.changeState(Open(cb))
            throw CircuitBreakerReject()
        }
    }
}


class CircuitBreaker(
    val baseUrl: String = "localhost:4444",
    val failThreshold: Int = 4,
    val resetTimeoutMs: Long = 30_000,
    val halfOpenSuccThreshold: Int = 1
) {
    private val mutex = Mutex()

    private var state: State = State.Closed(this)
    var failureCount = 0
    var successCount = 0


    val http = HttpClient(Java) {
        install(ContentNegotiation) { json(Json) }
        expectSuccess = false
    }

    suspend fun routeRequest(city: String){
        state.processRequest(city)

    }

    fun changeState(newState: State){
        this.state = newState
    }
}