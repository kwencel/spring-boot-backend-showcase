package io.github.kwencel.backendshowcase.movie

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface MovieRepository: JpaRepository<Movie, MovieId> {

    @EntityGraph(attributePaths = ["shows"])
    fun <T> findWithEagerShowsById(id: MovieId, type: Class<T>): T?

    @EntityGraph(attributePaths = ["shows"])
    @Query("SELECT m FROM Movie m")
    fun <T> findWithEagerShowsAll(type: Class<T>): List<T>
}
