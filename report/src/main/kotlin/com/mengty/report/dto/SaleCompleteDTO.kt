package com.mengty.report.dto

import com.fasterxml.jackson.annotation.JsonAlias
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID



data class SaleCompleteRequest(
    val paymentMethodId: UUID,
    val total: BigDecimal,
    val items: List<SaleCompleteItemRequest>,
    val paymentId: Long? = null
)

data class SaleCompleteItemRequest(
    val itemCode: String,
    val itemName: String?,
    val unitPrice: BigDecimal,
    val qty: BigDecimal,
    val total: BigDecimal
)

data class SaleCompleteResponse(
    val invoiceId: UUID,
    val series: String,
    val total: BigDecimal
)

data class VoidInvoiceRequest(
    @JsonAlias("reason")
    val voidReason: String? = null,
    val voidedBy: String? = null
)

data class VoidInvoiceResponse(
    val invoiceId: UUID,
    val series: String?,
    val status: String,
    val refundAmount: BigDecimal,
    val message: String
)

data class SaleOrderDetailResponse(
    val id: UUID,
    val series: String?,
    val status: String,
    val total: Double,
    val totalQty: Float,
    val postDate: LocalDate,
    val dateCreated: LocalDateTime,
    val items: List<SaleOrderItemResponse>,
    val voidReason: String?,
    val voidedBy: String?,
    val voidedAt: LocalDateTime?,
    val refundAmount: BigDecimal
)

data class SaleOrderItemResponse(
    val id: UUID,
    val itemCode: String?,
    val itemName: String?,
    val itemVariantName: String?,
    val sku: String,
    val unitPrice: Double,
    val qty: Double,
    val total: Double
)
