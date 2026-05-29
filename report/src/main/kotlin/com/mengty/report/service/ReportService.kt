package com.mengty.report.service

import com.mengty.report.dto.ReportResponse
import com.mengty.report.dto.SaleSummaryResponse
import com.mengty.report.model.PosInvoice
import com.mengty.report.repository.PosInvoiceRepository
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class ReportService(
    private val repository: PosInvoiceRepository
) {

    fun getReportByDate(date: LocalDate): ReportResponse {
        val invoices = repository.findReportInvoicesByDate(date)

        val totalAmount = invoices
            .filter { it.posOrderStatus != "Voided" }
            .sumOf { it.subTotal }
        val totalCount = invoices.size

        return ReportResponse(
            totalAmount = totalAmount,
            totalCount = totalCount,
            invoices = invoices
        )
    }

    fun getSaleSummary(
        startDate: LocalDate,
        endDate: LocalDate,
        periodType: String,
        shift: String
    ): SaleSummaryResponse {
        val invoices = repository.findSaleSummaryInvoices(startDate, endDate)

        val completed = invoices.filter {
            it.posOrderStatus.equals("Completed", ignoreCase = true)
        }

        val voided = invoices.filter {
            it.posOrderStatus.equals("Voided", ignoreCase = true)
        }

        val cancelled = invoices.filter {
            it.posOrderStatus.equals("Cancelled", ignoreCase = true)
        }

        val grossSale = completed.sumOf { it.subTotal }
        val discountSubtotal = completed.sumOf {
            java.math.BigDecimal.valueOf(it.additionalDisAmount)
        }
        val netSale = grossSale.subtract(discountSubtotal)
        val servicechargeSubtotal = java.math.BigDecimal.ZERO
        val grandTotal = netSale.add(servicechargeSubtotal)
        val refundAmount = invoices.sumOf { it.refundAmount }
        val receiptNumbers = invoices.mapNotNull { it.series?.takeIf(String::isNotBlank) }

        return SaleSummaryResponse(
            startNumber = receiptNumbers.firstOrNull() ?: "-",
            endNumber = receiptNumbers.lastOrNull() ?: "-",
            totalBill = invoices.size,
            total = grandTotal,
            grossSale = grossSale,
            discountSubtotal = discountSubtotal,
            netSale = netSale,
            servicechargeSubtotal = servicechargeSubtotal,
            grandTotal = grandTotal,
            ordersCompleted = java.math.BigDecimal.valueOf(completed.size.toLong()),
            cancelCount = java.math.BigDecimal.valueOf(cancelled.size.toLong()),
            cancelTotal = cancelled.sumOf { it.subTotal },
            returnAmount = java.math.BigDecimal.ZERO,
            refundAmount = refundAmount,
            voidedOrders = java.math.BigDecimal.valueOf(voided.size.toLong())
        )
    }


}
