package io.github.kwencel.backendshowcase.show.view

import io.github.kwencel.backendshowcase.common.view.IdView
import io.github.kwencel.backendshowcase.movie.MovieId
import io.github.kwencel.backendshowcase.show.ShowId
import java.time.OffsetDateTime

interface ShowView {
    val id: ShowId
    val movie: IdView<MovieId>
    val date: OffsetDateTime
    val priceCents: Int
    val room: String
}
