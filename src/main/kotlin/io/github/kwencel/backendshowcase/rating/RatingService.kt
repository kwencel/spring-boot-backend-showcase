package io.github.kwencel.backendshowcase.rating

import io.github.kwencel.backendshowcase.movie.MovieId
import io.github.kwencel.backendshowcase.movie.MovieRepository
import org.springframework.stereotype.Service

@Service
class RatingService(private val movieRepository: MovieRepository,
                    private val ratingRepository: RatingRepository) {

    fun getUserRating(user: String, movieId: MovieId) = ratingRepository.findByUsernameAndMovieId(user, movieId)

    fun updateRating(user: String, movieId: MovieId, rating: Short) {
        val updatedRating = ratingRepository.findByUsernameAndMovieId(user, movieId)
            ?.apply { value = rating }
            ?: Rating(user, movieRepository.getById(movieId), rating)
        ratingRepository.save(updatedRating)
    }
}