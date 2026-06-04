package com.mengty.report.controller

import com.mengty.report.dto.CreatePaymentRequest
import com.mengty.report.dto.CreatePaymentResponse
import com.mengty.report.dto.BakongCheckResponse
import com.mengty.report.dto.PaymentHistoryResponse
import com.mengty.report.dto.PaymentStatusResponse
import com.mengty.report.config.AppRole
import com.mengty.report.config.RequireRoles
import com.mengty.report.service.PaymentService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/payments")
@RequireRoles(AppRole.ADMIN, AppRole.MANAGER, AppRole.CASHIER)
class PaymentController(
        private val paymentService: PaymentService
) {
    @PostMapping
    fun createPayment(
            @RequestBody request: CreatePaymentRequest
    ): CreatePaymentResponse {
        return paymentService.createPayment(request)
    }

    @GetMapping
    fun getPaymentHistory(): List<PaymentHistoryResponse> {
        return paymentService.getPaymentHistory()
    }

    @GetMapping("/{id}")
    fun getPaymentStatus(
            @PathVariable id: Long
    ): PaymentStatusResponse {
        return paymentService.getPaymentStatus(id)
    }

    @GetMapping("/{id}/check")
    fun checkPayment(
            @PathVariable id: Long
    ): BakongCheckResponse {
        return paymentService.checkPayment(id)
    }
}
