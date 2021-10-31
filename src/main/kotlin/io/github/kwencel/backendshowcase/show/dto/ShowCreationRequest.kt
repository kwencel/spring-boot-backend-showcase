package io.github.kwencel.backendshowcase.show.dto

import io.github.kwencel.backendshowcase.movie.MovieId
import java.time.OffsetDateTime

data class ShowCreationRequest(val movieId: MovieId,
                               val date: OffsetDateTime,
                               val priceCents: Int,
                               val room: String)
