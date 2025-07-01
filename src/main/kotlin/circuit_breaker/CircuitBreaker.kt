package swa.circuit_breaker

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Duration
import java.time.Instant

sealed interface State {
    suspend fun processRequest()

    class ClosedState(private val cb: CircuitBreaker): State { override suspend fun processRequest() {} }
    class OpenLightState(private val cb: CircuitBreaker): State { override suspend fun processRequest() {} }
    class HalfOpenState(private val cb: CircuitBreaker): State { override suspend fun processRequest() {} }
}


class CircuitBreakerOpen : RuntimeException("Circuit is OPEN – call rejected")


class CircuitBreaker(
    private val failThreshold: Int = 4,
    private val resetTimeoutMs: Long = 30_000,
    private val halfOpenMaxSucc: Int = 1
) {
    private val mutex = Mutex()

    private var state: State = State.ClosedState(this)
    private var failureCount = 0
    private var successCount = 0
    private var openedAt: Instant = Instant.MIN



}