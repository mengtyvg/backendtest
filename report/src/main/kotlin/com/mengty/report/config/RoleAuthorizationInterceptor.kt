package com.mengty.report.config

import com.mengty.report.model.AppUser
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor

@Component
class RoleAuthorizationInterceptor : HandlerInterceptor {
    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): Boolean {
        if (handler !is HandlerMethod) {
            return true
        }

        val requiredRoles = handler.getMethodAnnotation(RequireRoles::class.java)
            ?: handler.beanType.getAnnotation(RequireRoles::class.java)
            ?: return true

        val authenticatedUser = request.getAttribute("authenticatedUser") as? AppUser
        if (authenticatedUser == null) {
            forbidden(response, "Authenticated user is required.")
            return false
        }

        val userRole = authenticatedUser.role.removePrefix("ROLE_").uppercase()
        val isAllowed = requiredRoles.value.any { it.name == userRole }
        if (!isAllowed) {
            forbidden(response, "You do not have permission to access this resource.")
            return false
        }

        return true
    }

    private fun forbidden(response: HttpServletResponse, message: String) {
        response.status = HttpStatus.FORBIDDEN.value()
        response.contentType = "application/json"
        response.writer.write("""{"message":"$message"}""")
    }
}
