package com.mengty.report.controller

import com.mengty.report.dto.PosInvoiceDTO
import com.mengty.report.config.AppRole
import com.mengty.report.config.RequireRoles
import com.mengty.report.service.PosInvoiceService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/test")
@RequireRoles(AppRole.ADMIN)
class PosInvoiceControllerTEST(
    private val posInvoiceService: PosInvoiceService
){
    @GetMapping
    fun mengty(): List<PosInvoiceDTO>{
        return posInvoiceService.mengty()
    }
}
