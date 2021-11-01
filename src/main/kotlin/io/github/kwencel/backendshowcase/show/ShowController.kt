package io.github.kwencel.backendshowcase.show

import io.github.kwencel.backendshowcase.exception.ResourceNotFoundException
import io.github.kwencel.backendshowcase.show.ShowController.Companion.path
import io.github.kwencel.backendshowcase.show.dto.ShowCreationRequest
import io.github.kwencel.backendshowcase.show.dto.ShowDto
import io.github.kwencel.backendshowcase.show.dto.toDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.http.HttpHeaders.LOCATION
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.PERMANENT_REDIRECT
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.bind.annotation.RequestMethod.DELETE
import org.springframework.web.bind.annotation.RequestMethod.GET
import java.net.URI

@RestController
@RequestMapping(path)
class ShowController(private val showService: ShowService) {

    @GetMapping
    @Operation(summary = "Get all shows")
    @ApiResponse(responseCode = "200")
    fun getAll(): List<ShowDto> = showService.viewAll().map { it.toDto() }

    @GetMapping("/{id}")
    @Operation(summary = "Get a particular show")
    @Parameter(name = "id", description = "ID of the show to get", required = true)
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Show has been found"),
        ApiResponse(responseCode = "404", description = "Show does not exist", content = [Content()])
    ])
    fun get(@PathVariable("id") id: ShowId): ShowDto {
        return showService.viewById(id)?.toDto() ?: throw ResourceNotFoundException(id)
    }

    @RequestMapping(path = ["/{id}/movie"], method = [GET, DELETE])
    @Operation(summary = "Redirects to the movie-related endpoint")
    @Parameter(name = "id", description = "ID of the show", required = true)
    @ApiResponses(value = [
        ApiResponse(responseCode = "301", description = "Movie has been found", content = [Content()], headers = [
            Header(name = "Location", description = "URI to access the requested movie", required = true)
        ]),
        ApiResponse(responseCode = "404", description = "Show does not exist", content = [Content()])
    ])
    fun redirectToMovie(@PathVariable("id") id: ShowId): ResponseEntity<Unit> {
        val movieId = showService.getMovieId(id) ?: throw ResourceNotFoundException(id)
        return ResponseEntity.status(PERMANENT_REDIRECT).header(LOCATION, "/api/movies/$movieId").build()
    }

    @PostMapping
    @Operation(summary = "Add a new show")
    @ApiResponses(value = [
        ApiResponse(responseCode = "201", description = "Show has been added", content = [Content()], headers = [
            Header(name = "Location", description = "URI to the created show", required = true)
        ]),
        ApiResponse(responseCode = "401", description = "You are not authenticated", content = [Content()]),
        ApiResponse(responseCode = "403", description = "You are unauthorized to do this operation", content = [Content()]),
    ])
    fun create(@RequestBody request: ShowCreationRequest): ResponseEntity<Unit> {
        val id = showService.create(request).id
        return ResponseEntity.created(URI.create("${path}/$id")).build()
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a particular show")
    @Parameter(name = "id", description = "ID of the show to delete", required = true)
    @ApiResponses(value = [
        ApiResponse(responseCode = "204", description = "Movie has been deleted", content = [Content()]),
        ApiResponse(responseCode = "401", description = "You are not authenticated", content = [Content()]),
        ApiResponse(responseCode = "403", description = "You are unauthorized to do this operation", content = [Content()]),
        ApiResponse(responseCode = "404", description = "Movie does not exist", content = [Content()])
    ])
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable("id") id: ShowId) {
        try {
            return showService.delete(id)
        } catch (e: EmptyResultDataAccessException) {
            throw ResourceNotFoundException(id)
        }
    }

    companion object {
        const val path = "/api/shows"
    }
}