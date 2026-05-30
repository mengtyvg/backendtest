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

    fun createUser(
        username: String,
        password: String,
        displayName: String,
        role: String,
        defaultPage: String,
        status: Boolean
    ): AppUserDTO {
        val cleanUsername = username.trim()
        val cleanDisplayName = displayName.trim()

        if (cleanUsername.isBlank() || password.isBlank() || cleanDisplayName.isBlank()) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Username, password, and display name are required."
            )
        }

        if (appUserRepository.existsByUsernameIgnoreCase(cleanUsername)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Username already exists.")
        }

        return toDTO(
            appUserRepository.save(
                AppUser(
                    username = cleanUsername,
                    passwordHash = requireNotNull(passwordEncoder.encode(password)),
                    displayName = cleanDisplayName,
                    role = role.trim(),
                    defaultPage = defaultPage.trim(),
                    status = status
                )
            )
        )
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
