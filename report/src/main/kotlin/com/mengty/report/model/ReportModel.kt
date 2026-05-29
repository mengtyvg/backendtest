package com.mengty.report.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "pos_invoice")
data class PosInvoice(
    @Id
    val id: UUID,

    val series: String?,

    @Column(name = "pos_order_status")
    val posOrderStatus: String,

    @Column(name = "is_receipt_print")
    val isReceiptPrint: Boolean = false,

    @Column(name = "additional_dis_amount")
    val additionalDisAmount: Double = 0.0,

    @Column(name = "additional_dis_percent")
    val additionalDisPercent: Double = 0.0,

    val total: Double,

    @Column(name = "sub_total")
    val subTotal: BigDecimal,

    @Column(name = "total_qty")
    val totalQty: Float,

    @Column(name = "post_date")
    val postDate: LocalDate,

    @Column(name = "date_created")
    val dateCreated: LocalDateTime,

    @Column(name = "voided_at")
    val voidedAt: LocalDateTime? = null,

    @Column(name = "voided_by")
    val voidedBy: String? = null,

    @Column(name = "void_reason")
    val voidReason: String? = null,

    @Column(name = "refund_amount")
    val refundAmount: BigDecimal = BigDecimal.ZERO
)
