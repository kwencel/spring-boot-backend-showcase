package io.github.kwencel.backendshowcase.movie

import io.github.kwencel.backendshowcase.movie.dto.MovieDto
import io.github.kwencel.backendshowcase.rating.RatingService
import io.github.kwencel.backendshowcase.security.SecurityConfig
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.security.test.context.support.WithAnonymousUser
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBodyList

@WebFluxTest(controllers = [MovieController::class])
@Import(MovieService::class, SecurityConfig::class)
internal class MovieControllerMockedDbTests {

	@MockBean
	private lateinit var movieRepository: MovieRepository

	@MockBean
	private lateinit var ratingService: RatingService

	@Autowired
	private lateinit var webClient: WebTestClient

	@Test
	fun `get all (empty list)`() {
		`when`(movieRepository.findAll()).thenReturn(emptyList())

		webClient.get()
			.uri(MovieController.path)
			.exchange()
			.expectStatus().isOk
			.expectBodyList<MovieDto>()
			.hasSize(0)
	}

}
