package io.github.kwencel.backendshowcase.movie

import io.github.kwencel.backendshowcase.exception.MovieDetailsDisabledException
import io.github.kwencel.backendshowcase.exception.MovieRatingNotFoundException
import io.github.kwencel.backendshowcase.exception.ResourceNotFoundException
import io.github.kwencel.backendshowcase.movie.MovieController.Companion.path
import io.github.kwencel.backendshowcase.movie.detail.MovieDetailProvider
import io.github.kwencel.backendshowcase.movie.dto.MovieCreationRequest
import io.github.kwencel.backendshowcase.movie.dto.MovieDto
import io.github.kwencel.backendshowcase.movie.dto.toDto
import io.github.kwencel.backendshowcase.rating.RatingService
import io.github.kwencel.backendshowcase.rating.dto.RatingUpdateRequest
import io.github.kwencel.backendshowcase.show.dto.ShowDto
import io.github.kwencel.backendshowcase.show.dto.toDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.net.URI
import java.security.Principal
import javax.validation.Valid

@RestController
@RequestMapping(path)
class MovieController(private val movieService: MovieService,
                      private val movieDetailProvider: MovieDetailProvider<ImdbId>?,
                      private val ratingService: RatingService) {

    @GetMapping
    @Operation(summary = "Get all movies")
    @ApiResponse(responseCode = "200")
    fun getAll(): List<MovieDto> = movieService.viewAll().map { it.toDto() }

    @GetMapping("/{id}")
    @Operation(summary = "Get a particular movie")
    @Parameter(name = "id", description = "ID of the movie to get", required = true)
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Movie has been found"),
        ApiResponse(responseCode = "404", description = "Movie does not exist", content = [Content()])
    ])
    fun get(@PathVariable("id") id: MovieId): MovieDto {
        return movieService.viewById(id)?.toDto() ?: throw ResourceNotFoundException(id)
    }

    @GetMapping("/{id}/details")
    @Operation(summary = "Fetch detailed movie information from OMDb. Response bodies can vary, as OMDb does not provide a schema for their API.")
    @Parameter(name = "id", description = "ID of the movie to get the details for", required = true)
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Movie details has been fetched successfully", content = [Content()]),
        ApiResponse(responseCode = "404", description = "Movie does not exist", content = [Content()]),
        ApiResponse(responseCode = "503", description = "Movie details fetching feature is currently unavailable", content = [Content()])
    ])
    fun getDetails(@PathVariable("id") id: MovieId): Mono<ResponseEntity<Flux<DataBuffer>>> {
        val imdbId = movieService.getImdbId(id) ?: throw ResourceNotFoundException(id)
        return movieDetailProvider?.getDetails(imdbId) ?: throw MovieDetailsDisabledException()
    }

    @GetMapping("/{id}/shows")
    @Operation(summary = "Get shows for a particular movie")
    @Parameter(name = "id", description = "ID of the movie to get the shows for", required = true)
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Shows has been returned successfully"),
        ApiResponse(responseCode = "404", description = "Movie does not exist"),
    ])
    fun getShows(@PathVariable("id") id: MovieId): List<ShowDto> {
        val movie = movieService.viewById(id) ?: throw ResourceNotFoundException(id)
        return movie.shows.map { it.toDto() }
    }

    @PostMapping
    @Operation(summary = "Add a new movie")
    @ApiResponses(value = [
        ApiResponse(responseCode = "201", description = "Movie has been added", content = [Content()], headers = [
            Header(name = "Location", description = "URI to the created movie", required = true)
        ]),
        ApiResponse(responseCode = "401", description = "You are not authenticated", content = [Content()]),
        ApiResponse(responseCode = "403", description = "You are unauthorized to do this operation", content = [Content()]),
    ])
    fun create(@RequestBody request: MovieCreationRequest): ResponseEntity<Unit> {
        val id = movieService.create(request).id
        return ResponseEntity.created(URI.create("$path/$id")).build()
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a particular movie")
    @Parameter(name = "id", description = "ID of the movie to delete", required = true)
    @ApiResponses(value = [
        ApiResponse(responseCode = "204", description = "Movie has been deleted", content = [Content()]),
        ApiResponse(responseCode = "401", description = "You are not authenticated", content = [Content()]),
        ApiResponse(responseCode = "403", description = "You are unauthorized to do this operation", content = [Content()]),
        ApiResponse(responseCode = "404", description = "Movie does not exist", content = [Content()])
    ])
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable("id") id: MovieId) {
        try {
            return movieService.delete(id)
        } catch (e: EmptyResultDataAccessException) {
            throw ResourceNotFoundException(id)
        }
    }

    @PutMapping("/{id}/rating")
    @Operation(summary = "Rate a particular movie")
    @Parameter(name = "id", description = "ID of the movie to rate", required = true)
    @ApiResponses(value = [
        ApiResponse(responseCode = "204", description = "Movie has been rated", content = [Content()]),
        ApiResponse(responseCode = "401", description = "You are not authenticated", content = [Content()]),
        ApiResponse(responseCode = "404", description = "Movie or rating does not exist", content = [Content()])
    ])
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun updateRating(@PathVariable("id") id: MovieId, @RequestBody @Valid request: RatingUpdateRequest, principal: Principal) {
        if (!movieService.exists(id)) throw ResourceNotFoundException(id)
        ratingService.updateRating(principal.name, id, request.rating)
    }

    @GetMapping("/{id}/rating")
    fun getRating(@PathVariable("id") id: MovieId, principal: Principal): Short {
        if (!movieService.exists(id)) throw ResourceNotFoundException(id)
        return ratingService.getUserRating(principal.name, id)?.value ?: throw MovieRatingNotFoundException(id)
    }

    companion object {
        const val path = "/api/movies"
    }
}
