package io.github.kwencel.backendshowcase.exception

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.web.WebProperties
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler
import org.springframework.boot.web.reactive.error.ErrorAttributes
import org.springframework.context.ApplicationContext
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.codec.ServerCodecConfigurer
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.RequestPredicates
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.RouterFunctions
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.server.ResponseStatusException


@Component
@Order(-2)
class GlobalExceptionHandler(
    errorAttributes: ErrorAttributes,
    resources: WebProperties.Resources,
    applicationContext: ApplicationContext,
    codecConfigurer: ServerCodecConfigurer
) : AbstractErrorWebExceptionHandler(errorAttributes, resources, applicationContext) {

    init {
        @Suppress("LeakingThis") setMessageReaders(codecConfigurer.readers)
        @Suppress("LeakingThis") setMessageWriters(codecConfigurer.writers)
    }

    override fun getRoutingFunction(errorAttributes: ErrorAttributes): RouterFunction<ServerResponse> {
        return RouterFunctions.route(RequestPredicates.all()) {
            logger.debug("Handling error of request $it")

            when (val error: Throwable? = errorAttributes.getError(it)) {
                is CustomHttpException ->
                    ServerResponse.status(error.httpStatus).bodyValue(wrap(CustomErrorResponse(error)))
                is ResponseStatusException ->
                    ServerResponse.status(error.status).bodyValue(wrap(CustomBasicErrorResponse()))
                else ->
                    ServerResponse.status(INTERNAL_SERVER_ERROR).bodyValue(wrap(CustomBasicErrorResponse()))
            }
        }
    }

    private fun wrap(dto: Any) = mapOf("error" to dto)

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}
