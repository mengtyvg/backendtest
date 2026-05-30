package com.mengty.report.dto

import java.time.LocalDateTime
import java.util.UUID

data class AppUserDTO(
    val id: UUID,
    val username: String,
    val displayName: String,
    val role: String,
    val defaultPage: String,
    val status: Boolean,
    val dateCreated: LocalDateTime
)

data class CreateAppUserRequest(
    val username: String,
    val displayName: String,
    val role: String,
    val password: String,
    val defaultPage: String,
    val status: Boolean
)
