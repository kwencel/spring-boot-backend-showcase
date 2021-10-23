package io.github.kwencel.backendshowcase.movie

import io.github.kwencel.backendshowcase.show.Show
import org.hibernate.annotations.NaturalId
import javax.persistence.*

typealias MovieId = Long
typealias ImdbId = String

@Entity
class Movie(

    @Column(nullable = false)
    var name: String,

    @Column(nullable = false)
    var durationMins: Short,

    @NaturalId
    @Column(length = 24, nullable = false, unique = true, updatable = false)
    var imdbId: ImdbId
) {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "movie_id_seq")
    @SequenceGenerator(name = "movie_id_seq", sequenceName = "movie_id_seq", allocationSize = 5)
    var id: MovieId = 0

    @OneToMany(mappedBy = "movie", cascade = [CascadeType.ALL], orphanRemoval = true)
    var shows: MutableSet<Show> = mutableSetOf()

    fun addShow(show: Show) {
        shows += show.also { it.movie = this }
    }

    fun removeShow(show: Show) {
        shows -= show
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Movie
        if (imdbId != other.imdbId) return false
        return true
    }

    override fun hashCode(): Int {
        return imdbId.hashCode()
    }

    override fun toString(): String {
        return "${this::class.simpleName}(id = $id , name = $name, imdbId = $imdbId)"
    }
}
