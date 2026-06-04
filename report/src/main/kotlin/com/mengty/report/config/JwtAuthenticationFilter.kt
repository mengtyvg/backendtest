package com.mengty.report.config

import com.mengty.report.repository.AppUserRepository
import com.mengty.report.service.JwtService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtService: JwtService,
    private val appUserRepository: AppUserRepository
) : OncePerRequestFilter() {

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        return !request.requestURI.startsWith("/api/") ||
            request.requestURI == "/api/users/login" ||
            request.method.equals("OPTIONS", ignoreCase = true)
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val header = request.getHeader(HttpHeaders.AUTHORIZATION)
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            unauthorized(response, "Bearer token is required.")
            return
        }

        val claims = try {
            jwtService.parseAndValidate(header.removePrefix(BEARER_PREFIX).trim())
        } catch (_: Exception) {
            unauthorized(response, "Bearer token is invalid or expired.")
            return
        }

        val user = appUserRepository.findById(claims.userId).orElse(null)
        if (user == null || !user.status) {
            unauthorized(response, "User account is inactive or no longer exists.")
            return
        }

        request.setAttribute("authenticatedUser", user)
        filterChain.doFilter(request, response)
    }

    private fun unauthorized(response: HttpServletResponse, message: String) {
        response.status = HttpStatus.UNAUTHORIZED.value()
        response.contentType = "application/json"
        response.writer.write("""{"message":"$message"}""")
    }

    private companion object {
        const val BEARER_PREFIX = "Bearer "
    }
}
