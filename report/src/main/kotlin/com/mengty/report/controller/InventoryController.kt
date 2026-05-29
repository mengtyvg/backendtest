package com.mengty.report.controller

import com.mengty.report.dto.CreateStockMovementRequest
import com.mengty.report.model.InventoryStockBalanceModel
import com.mengty.report.model.InventoryStockMovementModel
import com.mengty.report.service.InventoryService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/inventory")
@CrossOrigin("*")
class InventoryController(
    private val inventoryService: InventoryService
) {

    @GetMapping("/movements")
    fun getMovements(): List<InventoryStockMovementModel> {
        return inventoryService.getMovements()
    }

    @GetMapping("/balances")
    fun getBalances(): List<InventoryStockBalanceModel> {
        return inventoryService.getBalances()
    }

    @PostMapping("/movements")
    fun createMovement(
        @RequestBody request: CreateStockMovementRequest
    ): InventoryStockMovementModel {
        return inventoryService.createMovement(request)
    }
}