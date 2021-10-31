package io.github.kwencel.backendshowcase.movie.detail

import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.ResponseEntity
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono


interface MovieDetailProvider<T> {

    fun getDetails(id: T): Mono<ResponseEntity<Flux<DataBuffer>>>
}