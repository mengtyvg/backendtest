package com.mengty.report.config

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.Refill
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

@Component
class RateLimitFilter : OncePerRequestFilter() {

    private val buckets = ConcurrentHashMap<String, Bucket>()

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val key = rateLimitKey(request)
        val bucket = buckets.computeIfAbsent(key) {
            createBucket(request.requestURI)
        }

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response)
            return
        }

        response.status = 429
        response.contentType = "application/json"
        response.writer.write("""{"message":"Too many requests. Please try again later."}""")
    }

    private fun rateLimitKey(request: HttpServletRequest): String {
        val ip = request.getHeader("X-Forwarded-For")
            ?.split(",")
            ?.firstOrNull()
            ?.trim()
            ?: request.remoteAddr

        val group = when {
            request.requestURI.startsWith("/api/payments") -> "payments"
            request.requestURI.startsWith("/api/sale/complete") -> "sale-complete"
            request.requestURI.contains("/void") -> "void"
            request.requestURI.startsWith("/api/master") -> "master"
            else -> "general"
        }

        return "$ip:$group"
    }

    private fun createBucket(uri: String): Bucket {
        val limit = when {
            uri.startsWith("/api/payments") -> 30
            uri.startsWith("/api/sale/complete") -> 10
            uri.contains("/void") -> 5
            uri.startsWith("/api/master") -> 60
            else -> 120
        }

        return Bucket.builder()
            .addLimit(
                Bandwidth.classic(
                    limit.toLong(),
                    Refill.intervally(limit.toLong(), Duration.ofMinutes(1))
                )
            )
            .build()
    }
}