package com.mengty.report.service

import com.mengty.report.dto.AppUserDTO
import com.mengty.report.model.AppUser
import com.mengty.report.repository.AppUserRepository
import org.springframework.stereotype.Service

@Service
class AppUserService(
    private val appUserRepository: AppUserRepository
) {

    fun getUsers() =
        appUserRepository.findAll().map {
            AppUserDTO(
                it.id,
                it.username,
                it.displayName,
                it.role,
                it.status,
                it.dateCreated
            )
        }
}