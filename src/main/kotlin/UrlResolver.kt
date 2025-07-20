package swa

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.java.Java
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import swa.circuit_breaker.Logger


object UrlResolver {
    val logger = Logger(this::class.simpleName!!)

    val client = HttpClient(Java) { expectSuccess = false }

    val consulAddr = System.getenv("CONSUL_HTTP_ADDR")
        ?: "http://localhost:8500"

    suspend fun discoverService(name: String): String? {

        return try {
            val body: String = client.get("$consulAddr/v1/catalog/service/$name") {
                timeout { requestTimeoutMillis = 1_000 }
            }.body()

            val services = Json.decodeFromString<List<ConsulCatalogService>>(body)
            val service = services.firstOrNull() ?: return null

            val address = service.serviceAddress.ifBlank { service.address }
            val port = service.servicePort

            if (address.isNotBlank() && port != null){
                logger.log.info("Discovered service: $name")
                "http://$address:$port"

            }else{
                null
            }

        } catch (_: Exception) {
            logger.log.error("Failed to discover service: $name")
            null
        } finally {
            client.close()
        }
    }

    @Serializable
    private data class ConsulCatalogService(
        val serviceAddress: String = "",
        val address: String        = "",
        val servicePort: Int?      = null,
    )
}