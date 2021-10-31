package io.github.kwencel.backendshowcase.show.view

import io.github.kwencel.backendshowcase.common.view.IdView
import io.github.kwencel.backendshowcase.movie.MovieId

interface ShowMovieIdView {
    val movie: IdView<MovieId>
}