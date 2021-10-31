package io.github.kwencel.backendshowcase.common

import org.springframework.http.ResponseEntity
import java.util.Optional

fun <T> Optional<T>.toNullable(): T? = this.orElse(null)

fun <T> T?.toOptional(): Optional<T> = Optional.ofNullable(this)

fun <T : ResponseEntity.HeadersBuilder<*>> T.copyHeaders(source: ResponseEntity<*>, vararg headers: String): T {
    headers.map { it to source.headers.getOrEmpty(it) }
           .forEach { this.header(it.first, *(it.second.toTypedArray())) }
    return this
}
