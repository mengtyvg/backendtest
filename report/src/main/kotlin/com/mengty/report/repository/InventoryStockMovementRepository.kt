package com.mengty.report.repository

import com.mengty.report.model.InventoryStockMovementModel
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface InventoryStockMovementRepository : JpaRepository<InventoryStockMovementModel, UUID> {
    fun findByItemCodeOrderByMovementDateDesc(itemCode: String): List<InventoryStockMovementModel>
    fun findByItemVariantIdOrderByMovementDateDesc(itemVariantId: UUID): List<InventoryStockMovementModel>
}