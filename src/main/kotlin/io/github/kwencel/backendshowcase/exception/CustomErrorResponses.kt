package io.github.kwencel.backendshowcase.exception

class CustomErrorResponse(val code: Short, message: String) : CustomBasicErrorResponse(message) {
    constructor(ex: CustomHttpException) : this(ex.code, ex.message!!)
}

open class CustomBasicErrorResponse(val message: String = "Oops! Something went wrong.")
