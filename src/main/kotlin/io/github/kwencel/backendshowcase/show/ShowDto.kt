package io.github.kwencel.backendshowcase.show

import io.github.kwencel.backendshowcase.movie.MovieId
import io.github.kwencel.backendshowcase.show.view.ShowView
import java.time.OffsetDateTime

data class ShowDto(val id: ShowId,
                   val movieId: MovieId,
                   val date: OffsetDateTime,
                   val priceCents: Int,
                   val room: String)

fun Show.toDto(): ShowDto = ShowDto(id, movie.id, date, priceCents, room)

fun ShowView.toDto() = ShowDto(id, movie.id, date, priceCents, room)