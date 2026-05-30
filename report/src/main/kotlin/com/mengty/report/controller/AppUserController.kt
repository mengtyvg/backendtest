package com.mengty.report.controller

import com.mengty.report.dto.AppUserDTO
import com.mengty.report.dto.CreateAppUserRequest
import com.mengty.report.service.AppUserService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
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

    @PostMapping
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


    @GetMapping("/login")
    fun login(
        @RequestParam username: String,
        @RequestHeader("X-Password") password: String
    ): AppUserDTO {
        return appUserService.login(username.trim(), password)
    }


}
