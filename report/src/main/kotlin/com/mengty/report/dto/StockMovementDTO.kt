package com.mengty.report.dto


import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class CreateStockMovementRequest(
    val itemVariantId: UUID,
    val itemCode: String,
    val movementType: String, // IN or OUT
    val quantity: BigDecimal,
    val unitCost: BigDecimal? = BigDecimal.ZERO,
    val referenceNo: String? = null,
    val movementDate: LocalDate,
    val note: String? = null,
    val createdBy: String? = "admin"
)

data class StockMovementResponse(
    val id: UUID,
    val itemVariantId: UUID,
    val itemCode: String,
    val itemName: String? = null,
    val movementType: String,
    val quantity: BigDecimal,
    val unitCost: BigDecimal?,
    val referenceNo: String?,
    val movementDate: LocalDate,
    val note: String?,
    val createdBy: String?,
    val createdAt: LocalDateTime
)

data class StockBalanceResponse(
    val itemVariantId: UUID,
    val itemCode: String,
    val itemName: String? = null,
    val quantity: BigDecimal,
    val lastUpdated: LocalDateTime
)