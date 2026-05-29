package com.mengty.report.dto

import jakarta.persistence.Column
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class PosInvoiceDTO(
    val series: String?,
    val posOrderStatus: String,
    val total: Double,
    val totalQty: Float,
    val postDate: LocalDate
)