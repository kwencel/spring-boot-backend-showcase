package io.github.kwencel.backendshowcase.movie.dto

import io.github.kwencel.backendshowcase.movie.ImdbId
import io.github.kwencel.backendshowcase.movie.Movie
import io.github.kwencel.backendshowcase.movie.MovieId
import io.github.kwencel.backendshowcase.movie.view.MovieWithShowsView
import io.github.kwencel.backendshowcase.show.dto.ShowDto
import io.github.kwencel.backendshowcase.show.dto.toDto
import javax.validation.constraints.Size

data class MovieDto(val id: MovieId,
                    val name: String,
                    val durationMins: Short,
                    @Size(max = 24)
                    val imdbId: ImdbId,
                    val shows: List<ShowDto>)

fun Movie.toDto() = MovieDto(id, name, durationMins, imdbId, shows.map { it.toDto() })

fun MovieWithShowsView.toDto() = MovieDto(id, name, durationMins, imdbId, shows.map { it.toDto() })
