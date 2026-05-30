package com.mengty.report.repository

import com.mengty.report.model.AppUser
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AppUserRepository : JpaRepository<AppUser, UUID> {
    fun findByUsernameAndStatusTrue(username: String): AppUser?
}
