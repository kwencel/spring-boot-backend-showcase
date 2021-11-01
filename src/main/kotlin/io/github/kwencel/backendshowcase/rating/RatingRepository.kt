package io.github.kwencel.backendshowcase.rating

import io.github.kwencel.backendshowcase.movie.MovieId
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface RatingRepository: CrudRepository<Rating, RatingId> {

    fun findByUsernameAndMovieId(username: String, movieId: MovieId): Rating?

    @EntityGraph(attributePaths = ["movie"])
    @Query("SELECT r FROM Rating r")
    fun <T> findWithEagerMovieAll(type: Class<T>): List<T>

}
