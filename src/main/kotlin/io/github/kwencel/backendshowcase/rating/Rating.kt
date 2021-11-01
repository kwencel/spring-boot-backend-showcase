package io.github.kwencel.backendshowcase.rating

import io.github.kwencel.backendshowcase.movie.Movie
import org.hibernate.Hibernate
import org.hibernate.validator.constraints.Range
import javax.persistence.*

typealias RatingId = Long

@Entity
class Rating(

    var username: String,

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    var movie: Movie,

    @Range(min = 1, max = 5)
    var value: Short
) {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "rating_id_seq")
    @SequenceGenerator(name = "rating_id_seq", sequenceName = "rating_id_seq", allocationSize = 20)
    var id: RatingId = 0

    override fun hashCode(): Int {
        var result = username.hashCode()
        result = 31 * result + movie.hashCode()
        result = 31 * result + value
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Rating
        if (username != other.username) return false
        if (movie != other.movie) return false
        if (value != other.value) return false
        return true
    }

    override fun toString(): String {
        return "${this::class.simpleName}(id=$id, username=$username, movieId=${movie.id}, value=$value)"
    }
}
