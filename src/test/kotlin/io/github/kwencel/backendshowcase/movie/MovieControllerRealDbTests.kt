package io.github.kwencel.backendshowcase.movie

import io.github.kwencel.backendshowcase.movie.detail.MovieDetailProvider
import io.github.kwencel.backendshowcase.movie.dto.MovieCreationRequest
import io.github.kwencel.backendshowcase.movie.dto.MovieDto
import io.github.kwencel.backendshowcase.movie.dto.toDto
import io.github.kwencel.backendshowcase.rating.Rating
import io.github.kwencel.backendshowcase.rating.RatingRepository
import io.github.kwencel.backendshowcase.rating.dto.RatingUpdateRequest
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
import org.springframework.security.test.context.support.WithMockUser
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

    @Autowired
    private lateinit var ratingRepository: RatingRepository

    @MockBean
    private lateinit var movieDetailProvider: MovieDetailProvider<ImdbId>

    @Autowired
    private lateinit var webClient: WebTestClient

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    private lateinit var testMovieRecords: List<Movie>
    private lateinit var testShowRecords: List<Show>
    private lateinit var testRatingRecords: List<Rating>

    private val moviesTableName = Movie::class.simpleName!!.lowercase()
    private val showsTableName = Show::class.simpleName!!.lowercase()
    private val ratingTableName = Rating::class.simpleName!!.lowercase()

    @BeforeAll
    fun beforeAll() {
        testMovieRecords = movieRepository.findWithEagerShowsAll(Movie::class.java)
        testShowRecords = showRepository.findWithEagerMovieAll(Show::class.java)
        testRatingRecords = ratingRepository.findWithEagerMovieAll(Rating::class.java)
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
    @WithMockUser(roles = ["ADMIN"])
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
    @WithMockUser(roles = ["USER"])
    fun `create movie (no admin)`() {
        webClient.post()
            .uri(MovieController.path).exchange()
            .expectStatus().isForbidden
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
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
    @WithMockUser(roles = ["ADMIN"])
    fun `delete movie (not exists)`() {
        webClient.delete()
            .uri("${MovieController.path}/-1").exchange()
            .expectStatus().isNotFound
    }

    @Test
    @WithMockUser(roles = ["USER"])
    fun `delete movie (no admin)`() {
        webClient.delete()
            .uri("${MovieController.path}/-1").exchange()
            .expectStatus().isForbidden
    }

    @Test
    @WithMockUser(username = "user1")
    fun `add rating (movie exists)`() {
        try {
            webClient.put()
                .uri("${MovieController.path}/1/rating")
                .bodyValue(RatingUpdateRequest(5)).exchange()
                .expectStatus().isNoContent

            with(ratingRepository.findByUsernameAndMovieId("user1", 1)) {
                assertNotNull(this)
                assertEquals("user1", username)
                assertEquals(1, movie.id)
                assertEquals(5, this.value)
            }
        } finally {
            JdbcTestUtils.deleteFromTableWhere(jdbcTemplate, ratingTableName, "username = 'user1'")
            ensureDatabaseConsistency()
        }
    }

    @Test
    @WithMockUser(username = "user1")
    fun `add rating (movie does not exist)`() {
        try {
            webClient.put()
                .uri("${MovieController.path}/1/rating")
                .bodyValue(RatingUpdateRequest(5)).exchange()
                .expectStatus().isNoContent
        } finally {
            JdbcTestUtils.deleteFromTableWhere(jdbcTemplate, ratingTableName, "username = 'user1'")
            ensureDatabaseConsistency()
        }
    }

    @Test
    @WithMockUser(username = "user1")
    fun `add rating (below range)`() {
        webClient.put()
            .uri("${MovieController.path}/1/rating")
            .bodyValue(RatingUpdateRequest(0)).exchange()
            .expectStatus().isBadRequest
    }

    @Test
    @WithMockUser(username = "user1")
    fun `add rating (above range)`() {
        webClient.put()
            .uri("${MovieController.path}/1/rating")
            .bodyValue(RatingUpdateRequest(0)).exchange()
            .expectStatus().isBadRequest
    }

    @Test
    fun `add rating (unauthenticated)`() {
        webClient.put()
            .uri("${MovieController.path}/1/rating").exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun `get rating (unauthenticated)`() {
        webClient.get()
            .uri("${MovieController.path}/1/rating").exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    @WithMockUser(username = "user1")
    fun `get rating (movies does not exist)`() {
        webClient.get()
            .uri("${MovieController.path}/-1/rating").exchange()
            .expectStatus().isNotFound
    }

    @Test
    @WithMockUser(username = "user1")
    fun `get rating (rating does not exist)`() {
        webClient.get()
            .uri("${MovieController.path}/1/rating").exchange()
            .expectStatus().isNotFound
    }

    @Test
    @WithMockUser(username = "user2")
    fun `get rating (exist)`() {
        try {
            val movieReference = movieRepository.getById(3)
            ratingRepository.save(Rating("user2", movieReference, 3))

            webClient.get()
                .uri("${MovieController.path}/3/rating").exchange()
                .expectStatus().isOk
                .expectBody<Short>().isEqualTo(3)
        } finally {
            JdbcTestUtils.deleteFromTableWhere(jdbcTemplate, ratingTableName, "username = 'user2'")
            ensureDatabaseConsistency()
        }
    }

    private fun ensureDatabaseConsistency() {
        val dbMovieRecords = movieRepository.findWithEagerShowsAll(Movie::class.java)
        assertEquals(testMovieRecords, dbMovieRecords, "$moviesTableName table got inconsistent after this test run!")
        val dbShowRecords = showRepository.findWithEagerMovieAll(Show::class.java)
        assertEquals(testShowRecords, dbShowRecords, "$showsTableName table got inconsistent after this test run!")
        val dbRatingRecords = ratingRepository.findWithEagerMovieAll(Rating::class.java)
        assertEquals(testRatingRecords, dbRatingRecords, "$ratingTableName table got inconsistent after this test run!")
    }
}
