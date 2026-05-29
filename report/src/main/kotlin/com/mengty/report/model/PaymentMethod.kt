package com.mengty.report.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "pos_payment_method")
data class PosPaymentMethod(
    @Id
    val id: UUID,

    @Column(name = "payment_subtype")
    val paymentSubtype: String,

    @Column(name = "status")
    val status: Boolean = true,
)
