package io.github.kwencel.backendshowcase.exception

import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.*

open class CustomHttpException(
    val httpStatus: HttpStatus,
    val code: Short,
    message: String
) : RuntimeException(message)

class ResourceNotFoundException(id: Any)
    : CustomHttpException(NOT_FOUND, 1, "Resource with id=$id has not been found.")

class MovieDetailsDisabledException
    : CustomHttpException(SERVICE_UNAVAILABLE, 2, "Movie details fetching feature is currently unavailable")
