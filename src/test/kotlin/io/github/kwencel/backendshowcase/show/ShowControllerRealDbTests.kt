package io.github.kwencel.backendshowcase.show

import io.github.kwencel.backendshowcase.movie.Movie
import io.github.kwencel.backendshowcase.movie.MovieController
import io.github.kwencel.backendshowcase.movie.MovieRepository
import io.github.kwencel.backendshowcase.show.dto.ShowCreationRequest
import io.github.kwencel.backendshowcase.show.dto.ShowDto
import io.github.kwencel.backendshowcase.show.dto.toDto
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.jdbc.JdbcTestUtils
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.WebTestClient.ListBodySpec
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.test.web.reactive.server.expectBodyList
import java.time.OffsetDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@SpringBootTest
@AutoConfigureWebTestClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ShowControllerRealDbTests {

    @Autowired
    private lateinit var movieRepository: MovieRepository

    @Autowired
    private lateinit var showRepository: ShowRepository

    @Autowired
    private lateinit var webClient: WebTestClient

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    private lateinit var testMovieRecords: List<Movie>
    private lateinit var testShowRecords: List<Show>

    private val moviesTableName = Movie::class.simpleName!!.lowercase()
    private val showsTableName = Show::class.simpleName!!.lowercase()

    @BeforeAll
    fun beforeAll() {
        testMovieRecords = movieRepository.findWithEagerShowsAll(Movie::class.java)
        testShowRecords = showRepository.findWithEagerMovieAll(Show::class.java)
    }

    @Test
    fun `get all`() {
        webClient.get()
            .uri(ShowController.path).exchange()
            .expectStatus().isOk
            .expectBodyList<ShowDto>().isEqualTo<ListBodySpec<ShowDto>>(testShowRecords.map { it.toDto() })
    }

    @Test
    fun `get by id (exists)`() {
        webClient.get()
            .uri("${ShowController.path}/1").exchange()
            .expectStatus().isOk
            .expectBody<ShowDto>().isEqualTo(testShowRecords[0].toDto())
    }

    @Test
    fun `get by id (not exists)`() {
        webClient.get()
            .uri("${ShowController.path}/-1").exchange()
            .expectStatus().isNotFound
    }

    @Test
    fun `redirect to movie (exists)`() {
        webClient.get()
            .uri("${ShowController.path}/2/movie").exchange()
            .expectStatus().isPermanentRedirect
            .expectHeader().location("${MovieController.path}/5")
            .expectBody().isEmpty
    }

    @Test
    fun `redirect to movie (not exists)`() {
        webClient.get()
            .uri("${ShowController.path}/-1/movie").exchange()
            .expectStatus().isNotFound
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `create show`() {
        val movieId = 2L
        val date = OffsetDateTime.parse("2021-11-05T18:00:00+01")
        val room = "Room 5"
        val request = ShowCreationRequest(movieId, date, 1000, room)

        try {
            webClient.post()
                .uri(ShowController.path)
                .bodyValue(request).exchange()
                .expectStatus().isCreated
                .expectHeader().valueMatches("location", """^${ShowController.path}/\d+$""")
                .expectHeader().value("location") {
                    val newId = it.substringAfter("${ShowController.path}/").toLong()
                    val newShow = showRepository.findByIdOrNull(newId)
                    assertNotNull(newShow)
                    with(newShow) {
                        assertEquals(request.movieId, this.movie.id)
                        assertEquals(request.date, this.date)
                        assertEquals(request.priceCents, this.priceCents)
                        assertEquals(request.room, this.room)
                    }

                }
                .expectBody().isEmpty
        } finally {
            JdbcTestUtils.deleteFromTableWhere(jdbcTemplate, showsTableName,
                "movie_id = $movieId AND date = '$date' AND room = '$room'")
            ensureDatabaseConsistency()
        }
    }

    @Test
    @WithMockUser(roles = ["USER"])
    fun `create show (no admin)`() {
        webClient.post()
            .uri(ShowController.path).exchange()
            .expectStatus().isForbidden
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `delete (exists)`() {
        val movieId = 3L
        val date = OffsetDateTime.parse("2021-11-06T15:00:00+01")
        val room = "Room 7"

        try {
            val moviesCount = movieRepository.count()
            val showsCount = showRepository.count()
            val movieReference = movieRepository.getById(movieId)
            val show = showRepository.save(Show(movieReference, date, 700, room))

            webClient.delete()
                .uri("${ShowController.path}/${show.id}").exchange()
                .expectStatus().isNoContent
                .expectBody().isEmpty

            assertNotNull(movieRepository.findByIdOrNull(movieId))
            assertNull(showRepository.findByIdOrNull(show.id))
            assertEquals(moviesCount, movieRepository.count())
            assertEquals(showsCount, showRepository.count())
        } finally {
            JdbcTestUtils.deleteFromTableWhere(jdbcTemplate, showsTableName,
                "movie_id = $movieId AND date = '$date' AND room = '$room'")
            ensureDatabaseConsistency()
        }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `delete show (not exists)`() {
        webClient.delete()
            .uri("${ShowController.path}/-1").exchange()
            .expectStatus().isNotFound
    }

    @Test
    @WithMockUser(roles = ["USER"])
    fun `delete show (no admin)`() {
        webClient.delete()
            .uri("${ShowController.path}/-1").exchange()
            .expectStatus().isForbidden
    }

    private fun ensureDatabaseConsistency() {
        val dbMovieRecords = movieRepository.findWithEagerShowsAll(Movie::class.java)
        assertEquals(testMovieRecords, dbMovieRecords, "$moviesTableName table got inconsistent after this test run!")
        val dbShowRecords = showRepository.findWithEagerMovieAll(Show::class.java)
        assertEquals(testShowRecords, dbShowRecords, "$showsTableName table got inconsistent after this test run!")
    }
}
