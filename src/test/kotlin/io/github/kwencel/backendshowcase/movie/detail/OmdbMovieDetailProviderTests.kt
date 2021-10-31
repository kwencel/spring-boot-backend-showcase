package io.github.kwencel.backendshowcase.movie.detail

import io.github.kwencel.backendshowcase.movie.ImdbId
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


@WebFluxTest(OmdbMovieDetailProvider::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OmdbMovieDetailProviderTests {
    companion object {
        @JvmStatic
        private val server = MockWebServer().apply { start() }

        @JvmStatic
        @DynamicPropertySource
        fun properties(propertyRegistry: DynamicPropertyRegistry) {
            propertyRegistry.add("movie-detail-provider.omdb.url") { "http://localhost:${server.port}" }
        }
    }

    @TestConfiguration
    class TestConfig {
        @Bean
        fun webClientBuilder(): WebClient.Builder {
            return WebClient.builder()
        }
    }

    @Autowired
    private lateinit var movieDetailProvider: MovieDetailProvider<ImdbId>

    @AfterAll
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `proxy movie details from external API`() {
        val testResponse = "{\"Title\":\"The Fast and the Furious\"}"
        server.enqueue(
            MockResponse()
                .setBody(testResponse)
                .setHeader("Content-Type", APPLICATION_JSON_VALUE)
                .setHeader("Content-Length", testResponse.length)
                .setHeader("ETag", "expected-to-be-ignored")
        )

        val response = movieDetailProvider.getDetails("tt0232500").block()

        assertNotNull(response)
        with(response.headers) {
            assertEquals(APPLICATION_JSON, this.contentType)
            assertEquals(testResponse.length.toLong(), this.contentLength)
            assertEquals(2, this.size)
        }

        with(response.body) {
            assertNotNull(this)
            val mergedBody = mergeDataBuffers(this).block()
            assertNotNull(mergedBody)
            assertEquals(testResponse, String(mergedBody))
        }
    }

    private fun mergeDataBuffers(dataBufferFlux: Flux<DataBuffer>): Mono<ByteArray> {
        return DataBufferUtils.join(dataBufferFlux).map { dataBuffer ->
            ByteArray(dataBuffer.readableByteCount()).apply {
                dataBuffer.read(this)
                DataBufferUtils.release(dataBuffer)
            }
        }
    }
}
