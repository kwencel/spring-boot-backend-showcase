package io.github.kwencel.backendshowcase.show

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface ShowRepository: CrudRepository<Show, ShowId> {

    @EntityGraph(attributePaths = ["movie"])
    @Query("SELECT s FROM Show s")
    fun <T> findWithEagerMovieAll(type: Class<T>): List<T>
}
