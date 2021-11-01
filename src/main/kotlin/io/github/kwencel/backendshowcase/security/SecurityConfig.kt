package io.github.kwencel.backendshowcase.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.server.SecurityWebFilterChain


@Configuration
@EnableWebFluxSecurity
class SecurityConfig {

    @Bean
    fun userDetailsService(): MapReactiveUserDetailsService {
        val user1: UserDetails = User
            .withUsername("user1")
            .password(passwordEncoder().encode("user1"))
            .roles("USER")
            .build()
        val user2: UserDetails = User
            .withUsername("user2")
            .password(passwordEncoder().encode("user2"))
            .roles("USER")
            .build()
        val admin: UserDetails = User
            .withUsername("admin")
            .password(passwordEncoder().encode("admin"))
            .roles("USER,ADMIN")
            .build()
        return MapReactiveUserDetailsService(user1, user2, admin)
    }

    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http.authorizeExchange()
            .pathMatchers(*swaggerEndpoints).permitAll()
            .pathMatchers("/api/movies/*/rating").hasRole("USER")
            .pathMatchers(HttpMethod.GET,"/api/movies/**", "/api/shows/**").permitAll()
            .anyExchange().hasRole("ADMIN")
            .and().httpBasic()
            .and().csrf().disable()
            .build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    private val swaggerEndpoints = arrayOf(
        // -- Swagger UI v2
        "/v2/api-docs",
        "/swagger-resources",
        "/swagger-resources/**",
        "/configuration/ui",
        "/configuration/security",
        "/swagger-ui.html",
        "/webjars/**",
        // -- Swagger UI v3 (OpenAPI)
        "/v3/api-docs/**",
        "/swagger-ui/**"
    )
}