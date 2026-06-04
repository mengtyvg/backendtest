package com.mengty.report.controller

import com.mengty.report.dto.AppUserDTO
import com.mengty.report.dto.CreateAppUserRequest
import com.mengty.report.dto.LoginRequest
import com.mengty.report.dto.LoginResponse
import com.mengty.report.config.AppRole
import com.mengty.report.config.RequireRoles
import com.mengty.report.service.AppUserService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/users")
class AppUserController(
    private val appUserService: AppUserService
) {
    @GetMapping
    @RequireRoles(AppRole.ADMIN)
    fun getUsers(): List<AppUserDTO> {
        return appUserService.getUsers()
    }

    @PostMapping
    @RequireRoles(AppRole.ADMIN)
    fun createUser(@RequestBody request: CreateAppUserRequest): AppUserDTO {
        return appUserService.createUser(
            request.username,
            request.password,
            request.displayName,
            request.role,
            request.defaultPage,
            request.status
        )
    }


    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): LoginResponse {
        return appUserService.login(request.username.trim(), request.password)
    }


}
