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
@Table(name = "inventory_stock_movement")
data class InventoryStockMovementModel(
    @Id
    @Column(name = "id")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "item_variant_id")
    val itemVariantId: UUID,

    @Column(name = "item_code")
    val itemCode: String,

    @Column(name = "movement_type")
    val movementType: String,

    @Column(name = "quantity")
    val quantity: BigDecimal,

    @Column(name = "unit_cost")
    val unitCost: BigDecimal = BigDecimal.ZERO,

    @Column(name = "reference_no")
    val referenceNo: String? = null,

    @Column(name = "movement_date")
    val movementDate: LocalDate,

    @Column(name = "note")
    val note: String? = null,

    @Column(name = "created_by")
    val createdBy: String? = "admin",

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now()
)
