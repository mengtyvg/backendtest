package com.mengty.report.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "payments")
class Payment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    var itemVariantId: UUID,

    var amount: BigDecimal,

    var currency: String = "USD",

    var merchantAccount: String,

    @Column(columnDefinition = "TEXT")
    var khqr: String,

    var khqrMd5: String,

    var bakongHash: String? = null,

    var status: String = "PENDING",

    var createdAt: LocalDateTime = LocalDateTime.now(),

    var expiresAt: LocalDateTime,

    var paidAt: LocalDateTime? = null
)
