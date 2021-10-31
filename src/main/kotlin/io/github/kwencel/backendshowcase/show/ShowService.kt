package io.github.kwencel.backendshowcase.show

import io.github.kwencel.backendshowcase.movie.MovieId
import io.github.kwencel.backendshowcase.movie.MovieRepository
import io.github.kwencel.backendshowcase.show.dto.ShowCreationRequest
import io.github.kwencel.backendshowcase.show.dto.ShowDto
import io.github.kwencel.backendshowcase.show.dto.toDto
import io.github.kwencel.backendshowcase.show.view.ShowMovieIdView
import io.github.kwencel.backendshowcase.show.view.ShowView
import org.springframework.stereotype.Service

@Service
class ShowService(private val showRepository: ShowRepository,
                  private val movieRepository: MovieRepository) {

    fun viewById(id: ShowId) = showRepository.findById(id, ShowView::class.java)

    fun viewAll() = showRepository.findAllBy(ShowView::class.java)

    fun getMovieId(id: ShowId) = showRepository.findById(id, ShowMovieIdView::class.java)?.movie?.id

    fun create(request: ShowCreationRequest): ShowDto {
        val movieReference = movieRepository.getById(request.movieId)
        val show = showRepository.save(Show(movieReference, request.date, request.priceCents, request.room))
        return show.toDto()
    }

    fun delete(id: MovieId) {
        showRepository.deleteById(id)
    }

}