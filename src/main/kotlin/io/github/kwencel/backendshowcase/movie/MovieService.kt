package io.github.kwencel.backendshowcase.movie

import io.github.kwencel.backendshowcase.movie.dto.MovieCreationRequest
import io.github.kwencel.backendshowcase.movie.dto.MovieDto
import io.github.kwencel.backendshowcase.movie.dto.toDto
import io.github.kwencel.backendshowcase.movie.view.MovieImdbIdView
import io.github.kwencel.backendshowcase.movie.view.MovieWithShowsView
import org.springframework.stereotype.Service

@Service
class MovieService(private val movieRepository: MovieRepository) {

    fun viewById(id: MovieId) = movieRepository.findWithEagerShowsById(id, MovieWithShowsView::class.java)

    fun viewAll() = movieRepository.findWithEagerShowsAll(MovieWithShowsView::class.java)

    fun getImdbId(id: MovieId) = movieRepository.findById(id, MovieImdbIdView::class.java)?.imdbId

    fun create(request: MovieCreationRequest): MovieDto {
        val movie = movieRepository.save(Movie(request.name, request.durationMins, request.imdbId))
        return movie.toDto()
    }

    fun delete(id: MovieId) {
        movieRepository.deleteById(id)
    }
}
