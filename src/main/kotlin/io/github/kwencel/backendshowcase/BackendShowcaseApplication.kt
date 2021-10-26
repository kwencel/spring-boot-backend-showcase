package io.github.kwencel.backendshowcase

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
@OpenAPIDefinition(info = Info(title = "Backend showcase", version = "1.0", description = "Documentation APIs v1.0"))
class BackendShowcaseApplication

fun main(args: Array<String>) {
	runApplication<BackendShowcaseApplication>(*args)
}
