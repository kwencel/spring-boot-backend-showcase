package io.github.kwencel.backendshowcase.movie.view

import io.github.kwencel.backendshowcase.movie.MovieId

interface MovieView {
    val id: MovieId
    val name: String
    val durationMins: Short
    val imdbId: String
}