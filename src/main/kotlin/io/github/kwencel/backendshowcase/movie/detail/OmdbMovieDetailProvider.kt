package io.github.kwencel.backendshowcase.movie.detail

import io.github.kwencel.backendshowcase.common.copyHeaders
import io.github.kwencel.backendshowcase.movie.ImdbId
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.HttpHeaders.CONTENT_LENGTH
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.toEntityFlux
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

private const val omdbUrl = "movie-detail-provider.omdb.url"
private const val omdbApiKey = "movie-detail-provider.omdb.api-key"

@Component
@ConditionalOnProperty(omdbUrl, omdbApiKey)
class OmdbMovieDetailProvider(
    webClientBuilder: WebClient.Builder,
    @Value("\${$omdbUrl}")
    url: String,
    @Value("\${$omdbApiKey}")
    private val apiKey: String
) : MovieDetailProvider<ImdbId> {

    private val webClient = webClientBuilder.baseUrl(url).build()

    override fun getDetails(id: ImdbId): Mono<ResponseEntity<Flux<DataBuffer>>> {
        return webClient
            .get()
            .uri("/?apikey={omdbApiKey}&i={imdbId}", apiKey, id)
            .retrieve()
            .toEntityFlux<DataBuffer>()
            .map {
                ResponseEntity.ok()
                    .copyHeaders(it, CONTENT_TYPE, CONTENT_LENGTH)
                    .body(it.body)
            }
    }
}
