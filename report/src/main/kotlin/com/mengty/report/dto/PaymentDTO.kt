package com.mengty.report.dto

import java.util.UUID
import java.math.BigDecimal
import java.time.LocalDateTime

data class CreatePaymentRequest(
    val itemVariantId: UUID? = null,
    val items: List<CreatePaymentItemRequest> = emptyList()
)

data class CreatePaymentItemRequest(
    val itemVariantId: UUID,
    val qty: BigDecimal
)

data class CreatePaymentResponse(
    val paymentId: Long,
    val amount: String,
    val currency: String,
    val khqr: String,
    val md5: String,
    val status: String,
    val expiresAt: LocalDateTime
)

data class PaymentStatusResponse(
    val paymentId: Long,
    val status: String,
    val bakongHash: String? = null,
    val expiresAt: LocalDateTime? = null
)

data class BakongCheckResponse(
    val paymentId: Long,
    val status: String,
    val bakongHash: String?,
    val responseCode: Int?,
    val responseMessage: String?,
    val expiresAt: LocalDateTime? = null
)

data class PaymentHistoryResponse(
    val paymentId: Long,
    val amount: String,
    val currency: String,
    val status: String,
    val bakongHash: String?,
    val createdAt: LocalDateTime,
    val expiresAt: LocalDateTime,
    val paidAt: LocalDateTime?
)

class PaymentDTO {
}
