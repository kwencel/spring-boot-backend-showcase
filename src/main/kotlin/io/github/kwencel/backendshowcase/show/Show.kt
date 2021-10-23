package io.github.kwencel.backendshowcase.show

import io.github.kwencel.backendshowcase.movie.Movie
import java.time.OffsetDateTime
import javax.persistence.*

typealias ShowId = Long

@Entity
class Show(

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    var movie: Movie,

    var date: OffsetDateTime,

    var priceCents: Int,

    var room: String,
) {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "show_id_seq")
    @SequenceGenerator(name = "show_id_seq", sequenceName = "show_id_seq", allocationSize = 10)
    var id: ShowId = 0

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Show
        if (movie != other.movie) return false
        if (date != other.date) return false
        if (room != other.room) return false
        return true
    }

    override fun hashCode(): Int {
        var result = movie.hashCode()
        result = 31 * result + date.hashCode()
        result = 31 * result + room.hashCode()
        return result
    }

    override fun toString(): String {
        return "${this::class.simpleName}(id=$id, movieId=${movie.id}, date=$date," +
                "priceCents=$priceCents, room='$room')"
    }
}
