package com.mengty.report.service

import com.mengty.report.model.PosInvoice
import com.mengty.report.dto.CreatePaymentMethodRequest
import com.mengty.report.dto.CreateStockMovementRequest
import com.mengty.report.dto.SaleCompleteRequest
import com.mengty.report.dto.SaleCompleteResponse
import com.mengty.report.dto.SaleOrderDetailResponse
import com.mengty.report.dto.SaleOrderItemResponse
import com.mengty.report.dto.VoidInvoiceRequest
import com.mengty.report.dto.VoidInvoiceResponse
import com.mengty.report.model.PosInvoiceDetail
import com.mengty.report.model.PosPaymentMethod
import com.mengty.report.repository.ItemRepository
import com.mengty.report.repository.ItemVariantRepository
import com.mengty.report.repository.PosInvoiceDetailRepository
import com.mengty.report.repository.PaymentMethodRepository
import com.mengty.report.repository.PaymentRepository
import com.mengty.report.repository.PosInvoiceRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Service
class SaleService(
    private val paymentMethodRepository: PaymentMethodRepository,
    private val posInvoiceRepository: PosInvoiceRepository,
    private val posInvoiceDetailRepository: PosInvoiceDetailRepository,
    private val itemRepository: ItemRepository,
    private val itemVariantRepository: ItemVariantRepository,
    private val paymentRepository: PaymentRepository,
    private val inventoryService: InventoryService,
    private val telegramNotificationService: TelegramNotificationService
) {

    fun getPaymentMethods(): List<PosPaymentMethod> {
        return paymentMethodRepository.findByStatusTrueOrderByPaymentSubtypeAsc()
    }

    fun getAllPaymentMethods(): List<PosPaymentMethod> {
        return paymentMethodRepository.findAllByOrderByPaymentSubtypeAsc()
    }

    fun createPaymentMethod(request: CreatePaymentMethodRequest): PosPaymentMethod {
        val paymentSubtype = request.paymentSubtype.trim()
        if (paymentSubtype.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment method name is required.")
        }

        val existing = paymentMethodRepository.findFirstByPaymentSubtypeIgnoreCase(paymentSubtype)
        if (existing != null) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Payment method already exists.")
        }

        return paymentMethodRepository.save(
            PosPaymentMethod(
                id = UUID.randomUUID(),
                paymentSubtype = paymentSubtype,
                status = request.status
            )
        )
    }

    fun getTodayOrders(): List<PosInvoice> {
        return posInvoiceRepository.findReportInvoicesByDate(LocalDate.now())
    }

    fun getOrderDetail(invoiceId: UUID): SaleOrderDetailResponse {
        val invoice = posInvoiceRepository.findById(invoiceId)
            .orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice was not found.")
            }

        val items = posInvoiceDetailRepository.findByPosInvoiceIdOrderByDateCreatedAsc(invoiceId)
            .map { detail ->
                SaleOrderItemResponse(
                    id = detail.id,
                    itemCode = detail.itemCode,
                    itemName = detail.itemName,
                    itemVariantName = detail.itemVariantName,
                    sku = detail.sku,
                    unitPrice = detail.unitPrice,
                    qty = detail.qty,
                    total = detail.total
                )
            }

        return SaleOrderDetailResponse(
            id = invoice.id,
            series = invoice.series,
            status = invoice.posOrderStatus,
            total = invoice.total,
            totalQty = invoice.totalQty,
            postDate = invoice.postDate,
            dateCreated = invoice.dateCreated,
            items = items,
            voidReason = invoice.voidReason,
            refundAmount = invoice.refundAmount,
            voidedBy = invoice.voidedBy,
            voidedAt = invoice.voidedAt
        )
    }

    fun updatePaymentMethodStatus(id: UUID, status: Boolean): PosPaymentMethod {
        val existing = paymentMethodRepository.findById(id)
            .orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND, "Payment method was not found.")
            }

        return paymentMethodRepository.save(existing.copy(status = status))
    }

    @Transactional
    fun voidInvoice(invoiceId: UUID, request: VoidInvoiceRequest): VoidInvoiceResponse {
        val reason = request.voidReason?.trim()
        if (reason?.isBlank() ?: true) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Void reason is required.")
        }

        val invoice = posInvoiceRepository.findById(invoiceId)
            .orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice was not found.")
            }

        if (invoice.posOrderStatus == "Voided") {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invoice is already voided.")
        }

        if (invoice.posOrderStatus != "Completed") {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Only completed invoices can be voided.")
        }

        val updated = invoice.copy(
            posOrderStatus = "Voided",
            voidedAt = LocalDateTime.now(),
            voidedBy = request.voidedBy?.trim()?.takeIf { it.isNotBlank() } ?: "admin",
            voidReason = reason,
            refundAmount = invoice.subTotal.setScale(2, RoundingMode.HALF_UP)
        )

        val saved = posInvoiceRepository.save(updated)
        telegramNotificationService.notifyInvoiceVoided(
            series = saved.series,
            refundAmount = saved.refundAmount,
            reason = reason,
            voidedBy = saved.voidedBy,
            voidedAt = saved.voidedAt ?: LocalDateTime.now()
        )

        return VoidInvoiceResponse(
            invoiceId = saved.id,
            series = saved.series,
            status = saved.posOrderStatus,
            refundAmount = saved.refundAmount,
            message = "Invoice voided successfully."
        )
    }

    @Transactional
    fun completeSale(request: SaleCompleteRequest): SaleCompleteResponse {
        if (request.items.isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Please add at least one item before completing the sale.")
        }

        val paymentMethod = paymentMethodRepository.findById(request.paymentMethodId)
            .orElseThrow {
                ResponseStatusException(HttpStatus.BAD_REQUEST, "Please select an active payment method.")
            }

        if (!paymentMethod.status) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Please select an active payment method.")
        }

        val payment = request.paymentId?.let { paymentId ->
            val existing = paymentRepository.findById(paymentId)
                .orElseThrow {
                    ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment was not found.")
                }

            if (existing.status != "PAID") {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment is not paid yet.")
            }

            if (LocalDateTime.now().isAfter(existing.expiresAt)) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment QR has expired.")
            }

            existing
        }

        val now = LocalDateTime.now()
        val postDate = now.toLocalDate()
        val invoiceId = UUID.randomUUID()
        val series = "INV${(posInvoiceRepository.findLastInvoiceNumber() + 1).toString().padStart(7, '0')}"
        val total = request.total.setScale(2, RoundingMode.HALF_UP)
        val totalQty = request.items.fold(BigDecimal.ZERO) { sum, item -> sum + item.qty }

        val invoice = PosInvoice(
            id = invoiceId,
            series = series,
            posOrderStatus = "Completed",
            isReceiptPrint = false,
            additionalDisAmount = 0.0,
            additionalDisPercent = 0.0,
            total = total.toDouble(),
            subTotal = total,
            totalQty = totalQty.toFloat(),
            postDate = postDate,
            dateCreated = now
        )

        val details = request.items.map { saleItem ->
            val item = itemRepository.findByItemCodeAndStatusTrue(saleItem.itemCode)
                ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Item ${saleItem.itemCode} is inactive or no longer exists.")
            val variant = itemVariantRepository.findFirstActiveSaleVariantByItemId(item.id)
                ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Item ${saleItem.itemCode} does not have an active variant.")
            val qty = saleItem.qty.setScale(2, RoundingMode.HALF_UP)
            val unitPrice = saleItem.unitPrice.setScale(2, RoundingMode.HALF_UP)
            val lineTotal = saleItem.total.setScale(2, RoundingMode.HALF_UP)

            PosInvoiceDetail(
                id = UUID.randomUUID(),
                itemId = item.id,
                unitPrice = unitPrice.toDouble(),
                qty = qty.toDouble(),
                subTotal = lineTotal.toDouble(),
                cost = variant.cost,
                totalCost = variant.cost * qty.toDouble(),
                posInvoiceId = invoiceId,
                dateCreated = now,
                lastUpdated = now,
                total = lineTotal.toDouble(),
                postDate = postDate,
                itemCode = item.itemCode,
                itemName = item.itemName,
                itemVariantId = variant.id,
                itemVariantName = variant.name,
                sku = variant.sku
            )
        }

        posInvoiceRepository.save(invoice)
        posInvoiceDetailRepository.saveAll(details)
        details.forEach { detail ->
            val itemVariantId = detail.itemVariantId
                ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Sale item ${detail.itemCode} does not have a variant.")
            val itemCode = detail.itemCode
                ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Sale item does not have an item code.")

            inventoryService.createMovement(
                CreateStockMovementRequest(
                    itemVariantId = itemVariantId,
                    itemCode = itemCode,
                    movementType = "OUT",
                    quantity = BigDecimal.valueOf(detail.qty),
                    unitCost = BigDecimal.valueOf(detail.cost),
                    referenceNo = series,
                    movementDate = postDate,
                    note = "Sale completed",
                    createdBy = "system"
                )
            )
        }
        telegramNotificationService.notifyOrderCompleted(
            series = series,
            total = total,
            totalQty = totalQty,
            createdAt = now,
            paymentSubtype = paymentMethod.paymentSubtype
        )

        return SaleCompleteResponse(
            invoiceId = invoiceId,
            series = series,
            total = total
        )
    }
}
