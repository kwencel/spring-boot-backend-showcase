package io.github.kwencel.backendshowcase.movie

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.kwencel.backendshowcase.exception.ResourceNotFoundException
import io.github.kwencel.backendshowcase.movie.MovieController.Companion.path
import io.github.kwencel.backendshowcase.movie.dto.MovieCreationRequest
import io.github.kwencel.backendshowcase.movie.dto.MovieDto
import io.github.kwencel.backendshowcase.movie.dto.toDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI

@RestController
@RequestMapping(path)
class MovieController(private val movieService: MovieService) {

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

    @PostMapping
    @Operation(summary = "Add a new movie")
    @ApiResponse(responseCode = "201", description = "Movie has been added", content = [Content()], headers = [
        Header(name = "Location", description = "URI to the created movie", required = true)
    ])
    // TODO secure the endpoint
    fun create(@RequestBody request: MovieCreationRequest): ResponseEntity<Unit> {
        val id = movieService.create(request).id
        return ResponseEntity.created(URI.create("$path/$id")).build()
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a particular movie")
    @Parameter(name = "id", description = "ID of the movie to delete", required = true)
    @ApiResponses(value = [
        ApiResponse(responseCode = "204", description = "Movie has been deleted", content = [Content()]),
        ApiResponse(responseCode = "404", description = "Movie does not exist", content = [Content()])
    ])
    @ResponseStatus(HttpStatus.NO_CONTENT)
    // TODO secure the endpoint
    fun delete(@PathVariable("id") id: MovieId) {
        try {
            return movieService.delete(id)
        } catch (e: EmptyResultDataAccessException) {
            throw ResourceNotFoundException(id)
        }
    }

    companion object {
        const val path = "/api/movies"
    }
}
