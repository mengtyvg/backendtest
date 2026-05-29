package com.mengty.report.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "inventory_stock_balance")
data class InventoryStockBalanceModel(
    @Id
    @Column(name = "id")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "item_variant_id")
    val itemVariantId: UUID,

    @Column(name = "item_code")
    val itemCode: String,

    @Column(name = "quantity")
    val quantity: BigDecimal = BigDecimal.ZERO,

    @Column(name = "last_updated")
    val lastUpdated: LocalDateTime = LocalDateTime.now()
)
