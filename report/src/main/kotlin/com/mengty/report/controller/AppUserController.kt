package com.mengty.report.controller

import com.mengty.report.dto.AppUserDTO
import com.mengty.report.model.AppUser
import com.mengty.report.service.AppUserService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/users")
class AppUserController(
    private val appUserService: AppUserService
) {
    @GetMapping
    fun getUsers(): List<AppUserDTO> {
        return appUserService.getUsers()
    }
}