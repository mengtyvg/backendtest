package com.mengty.report.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "app_user")
data class AppUser(
    @Id
    @Column(name = "id")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "username")
    val username: String,

    @Column(name = "password_hash")
    val passwordHash: String,

    @Column(name = "display_name")
    val displayName: String,

    @Column(name = "role")
    val role: String,

    @Column(name = "default_page")
    val defaultPage: String,

    @Column(name = "status")
    val status: Boolean = true,

    @Column(name = "date_created")
    val dateCreated: LocalDateTime = LocalDateTime.now(),

    @Column(name = "last_updated")
    val lastUpdated: LocalDateTime = LocalDateTime.now()
)
