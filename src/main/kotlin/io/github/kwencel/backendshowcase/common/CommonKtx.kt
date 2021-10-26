package io.github.kwencel.backendshowcase.common

import java.util.Optional

fun <T> Optional<T>.toNullable(): T? = this.orElse(null)

fun <T> T?.toOptional(): Optional<T> = Optional.ofNullable(this)