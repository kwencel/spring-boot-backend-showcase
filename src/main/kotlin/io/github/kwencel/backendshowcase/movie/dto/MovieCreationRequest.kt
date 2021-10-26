package io.github.kwencel.backendshowcase.movie.dto

import io.github.kwencel.backendshowcase.movie.ImdbId

data class MovieCreationRequest(val name: String, val durationMins: Short, val imdbId: ImdbId)
