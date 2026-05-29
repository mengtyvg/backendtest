package com.mengty.report.controller

import com.mengty.report.dto.CreatePaymentMethodRequest
import com.mengty.report.dto.SaleCompleteRequest
import com.mengty.report.dto.SaleCompleteResponse
import com.mengty.report.dto.PaymentMethodStatusRequest
import com.mengty.report.dto.SaleOrderDetailResponse
import com.mengty.report.dto.VoidInvoiceRequest
import com.mengty.report.dto.VoidInvoiceResponse
import com.mengty.report.model.PosInvoice
import org.springframework.web.bind.annotation.*

import com.mengty.report.model.PosPaymentMethod
import com.mengty.report.service.SaleService
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/sale")
@CrossOrigin("*")
class SaleController(
    private val saleService: SaleService
) {

    @GetMapping("/payment-methods")
    fun getPaymentMethods(): List<PosPaymentMethod> {
        return saleService.getPaymentMethods()
    }

    @GetMapping("/payment-methods/all")
    fun getAllPaymentMethods(): List<PosPaymentMethod> {
        return saleService.getAllPaymentMethods()
    }

    @PostMapping("/payment-methods")
    fun createPaymentMethod(@RequestBody request: CreatePaymentMethodRequest): PosPaymentMethod {
        return saleService.createPaymentMethod(request)
    }

    @GetMapping("/orders/today")
    fun getTodayOrders(): List<PosInvoice> {
        return saleService.getTodayOrders()
    }

    @GetMapping("/orders/{invoiceId}")
    fun getOrderDetail(@PathVariable invoiceId: UUID): SaleOrderDetailResponse {
        return saleService.getOrderDetail(invoiceId)
    }

    @PutMapping("/payment-methods/{id}/status")
    fun updatePaymentMethodStatus(
        @PathVariable id: UUID,
        @RequestBody request: PaymentMethodStatusRequest
    ): PosPaymentMethod {
        return saleService.updatePaymentMethodStatus(id, request.status)
    }

    @PostMapping("/complete")
    fun completeSale(@RequestBody request: SaleCompleteRequest): SaleCompleteResponse {
        return saleService.completeSale(request)
    }

    @PutMapping("/orders/{invoiceId}/void")
    fun voidInvoice(
        @PathVariable invoiceId: UUID,
        @RequestBody request: VoidInvoiceRequest
    ): VoidInvoiceResponse {
        return saleService.voidInvoice(invoiceId, request)
    }
}
