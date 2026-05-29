package com.mengty.report.repository

import com.mengty.report.model.InventoryStockBalanceModel
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface InventoryStockBalanceRepository : JpaRepository<InventoryStockBalanceModel, UUID> {
    fun findByItemVariantId(itemVariantId: UUID): InventoryStockBalanceModel?
    fun findByItemCode(itemCode: String): InventoryStockBalanceModel?
}