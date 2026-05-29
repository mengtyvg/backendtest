package com.mengty.report.service

import com.mengty.report.dto.CreateStockMovementRequest
import com.mengty.report.model.InventoryStockBalanceModel
import com.mengty.report.model.InventoryStockMovementModel
import com.mengty.report.repository.InventoryStockBalanceRepository
import com.mengty.report.repository.InventoryStockMovementRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
class InventoryService(
    private val movementRepository: InventoryStockMovementRepository,
    private val balanceRepository: InventoryStockBalanceRepository
) {

    fun getMovements(): List<InventoryStockMovementModel> {
        return movementRepository.findAll()
    }

    fun getBalances(): List<InventoryStockBalanceModel> {
        return balanceRepository.findAll()
    }

    @Transactional
    fun createMovement(request: CreateStockMovementRequest): InventoryStockMovementModel {
        if (request.quantity <= BigDecimal.ZERO) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be greater than zero.")
        }

        val movementType = request.movementType.uppercase()

        if (movementType != "IN" && movementType != "OUT") {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Movement type must be IN or OUT.")
        }

        val movement = InventoryStockMovementModel(
            itemVariantId = request.itemVariantId,
            itemCode = request.itemCode,
            movementType = movementType,
            quantity = request.quantity,
            unitCost = request.unitCost ?: BigDecimal.ZERO,
            referenceNo = request.referenceNo,
            movementDate = request.movementDate,
            note = request.note,
            createdBy = request.createdBy
        )

        val savedMovement = movementRepository.save(movement)

        val existingBalance = balanceRepository.findByItemVariantId(request.itemVariantId)

        val oldQty = existingBalance?.quantity ?: BigDecimal.ZERO
        val newQty = when (movementType) {
            "IN" -> oldQty + request.quantity
            "OUT" -> oldQty - request.quantity
            else -> oldQty
        }

        if (newQty < BigDecimal.ZERO) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Not enough stock.")
        }

        val balance = existingBalance?.copy(
            quantity = newQty,
            lastUpdated = LocalDateTime.now()
        ) ?: InventoryStockBalanceModel(
            itemVariantId = request.itemVariantId,
            itemCode = request.itemCode,
            quantity = newQty,
            lastUpdated = LocalDateTime.now()
        )

        balanceRepository.save(balance)

        return savedMovement
    }
}