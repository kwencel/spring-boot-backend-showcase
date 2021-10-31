package io.github.kwencel.backendshowcase.movie

import io.github.kwencel.backendshowcase.movie.detail.MovieDetailProvider
import io.github.kwencel.backendshowcase.movie.dto.MovieCreationRequest
import io.github.kwencel.backendshowcase.movie.dto.MovieDto
import io.github.kwencel.backendshowcase.movie.dto.toDto
import io.github.kwencel.backendshowcase.show.Show
import io.github.kwencel.backendshowcase.show.ShowRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.ResponseEntity
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.jdbc.JdbcTestUtils
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.WebTestClient.ListBodySpec
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.test.web.reactive.server.expectBodyList
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.ByteBuffer
import java.time.OffsetDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@SpringBootTest
@AutoConfigureWebTestClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MovieControllerRealDbTests {

    @Autowired
    private lateinit var movieRepository: MovieRepository

    @Autowired
    private lateinit var showRepository: ShowRepository

    @MockBean
    private lateinit var movieDetailProvider: MovieDetailProvider<ImdbId>

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
            .uri(MovieController.path).exchange()
            .expectStatus().isOk
            .expectBodyList<MovieDto>().isEqualTo<ListBodySpec<MovieDto>>(testMovieRecords.map { it.toDto() })
    }

    @Test
    fun `get by id (exists)`() {
        webClient.get()
            .uri("${MovieController.path}/1").exchange()
            .expectStatus().isOk
            .expectBody<MovieDto>().isEqualTo(testMovieRecords[0].toDto())
    }

    @Test
    fun `get by id (not exists)`() {
        webClient.get()
            .uri("${MovieController.path}/-1").exchange()
            .expectStatus().isNotFound
    }

    @Test
    fun `get details`() {
        val testResponse = "{\"Title\":\"The Fast and the Furious\"}"
        val testBuffer = ByteBuffer.wrap(testResponse.toByteArray())
        `when`(movieDetailProvider.getDetails("tt0232500")).thenReturn(
            Mono.just(ResponseEntity.ok(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(testBuffer))))
        )

        webClient.get()
            .uri("${MovieController.path}/1/details").exchange()
            .expectStatus().isOk
            .expectBody().json(testResponse)
    }

    @Test
    fun `create movie`() {
        val imdbId = "tt0816692"
        val request = MovieCreationRequest("Interstellar", 169, imdbId)

        try {
            webClient.post()
                .uri(MovieController.path)
                .bodyValue(request).exchange()
                .expectStatus().isCreated
                .expectHeader().valueMatches("location", """^${MovieController.path}/\d+$""")
                .expectHeader().value("location") {
                    val newId = it.substringAfter("${MovieController.path}/").toLong()
                    val newMovie = movieRepository.findByIdOrNull(newId)
                    assertNotNull(newMovie)
                    with(newMovie) {
                        assertEquals(request.name, this.name)
                        assertEquals(request.durationMins, this.durationMins)
                        assertEquals(request.imdbId, this.imdbId)
                    }

                }
                .expectBody().isEmpty
        } finally {
            JdbcTestUtils.deleteFromTableWhere(jdbcTemplate, moviesTableName, "imdb_id = '$imdbId'")
            ensureDatabaseConsistency()
        }
    }

    @Test
    fun `delete movie (exists)`() {
        val imdbId = "tt0372784"
        try {
            val moviesCount = movieRepository.count()
            val showsCount = showRepository.count()
            val movie = movieRepository.save(Movie("Batman: Begins", 140, imdbId))
            val show = showRepository.save(Show(movie, OffsetDateTime.parse("2005-07-29T19:00:00+02"), 900, "Room 5"))

            webClient.delete()
                .uri("${MovieController.path}/${movie.id}").exchange()
                .expectStatus().isNoContent
                .expectBody().isEmpty

            assertNull(movieRepository.findByIdOrNull(movie.id))
            assertNull(showRepository.findByIdOrNull(show.id))
            assertEquals(moviesCount, movieRepository.count())
            assertEquals(showsCount, showRepository.count())
        } finally {
            JdbcTestUtils.deleteFromTableWhere(jdbcTemplate, showsTableName,
                "movie_id = (SELECT id FROM movie WHERE imdb_id = '$imdbId')")
            JdbcTestUtils.deleteFromTableWhere(jdbcTemplate, moviesTableName, "imdb_id = '$imdbId'")
            ensureDatabaseConsistency()
        }
    }

    @Test
    fun `delete (not exists)`() {
        webClient.delete()
            .uri("${MovieController.path}/-1").exchange()
            .expectStatus().isNotFound
    }

    private fun ensureDatabaseConsistency() {
        val dbMovieRecords = movieRepository.findWithEagerShowsAll(Movie::class.java)
        assertEquals(testMovieRecords, dbMovieRecords, "$moviesTableName table got inconsistent after this test run!")
        val dbShowRecords = showRepository.findWithEagerMovieAll(Show::class.java)
        assertEquals(testShowRecords, dbShowRecords, "$showsTableName table got inconsistent after this test run!")
    }
}
