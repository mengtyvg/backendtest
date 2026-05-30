package com.mengty.report.service

import com.mengty.report.dto.AppUserDTO
import com.mengty.report.model.AppUser
import com.mengty.report.repository.AppUserRepository
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class AppUserService(
    private val appUserRepository: AppUserRepository
) {
    private val passwordEncoder = BCryptPasswordEncoder()

    fun getUsers() =
        appUserRepository.findAll().map(::toDTO)

    fun login(username: String, password: String): AppUserDTO {
        val user = appUserRepository.findByUsernameAndStatusTrue(username)
            ?: throw invalidCredentials()

        if (!passwordEncoder.matches(password, user.passwordHash)) {
            throw invalidCredentials()
        }

        return toDTO(user)
    }

    private fun invalidCredentials() =
        ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password.")

    private fun toDTO(user: AppUser) =
        AppUserDTO(
            user.id,
            user.username,
            user.displayName,
            user.role,
            user.status,
            user.dateCreated
        )
}
