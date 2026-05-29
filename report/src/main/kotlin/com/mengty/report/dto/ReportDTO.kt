package com.mengty.report.dto

import com.mengty.report.model.PosInvoice
import java.math.BigDecimal

data class ReportResponse(
    val totalAmount: BigDecimal,
    val totalCount: Int,
    val invoices: List<PosInvoice>
)

data class SaleSummaryResponse(
    val startNumber: String,
    val endNumber: String,
    val totalBill: Int,
    val total: BigDecimal,
    val grossSale: BigDecimal,
    val discountSubtotal: BigDecimal,
    val netSale: BigDecimal,
    val servicechargeSubtotal: BigDecimal,
    val grandTotal: BigDecimal,
    val ordersCompleted: BigDecimal,
    val cancelCount: BigDecimal,
    val cancelTotal: BigDecimal,
    val returnAmount: BigDecimal,
    val refundAmount: BigDecimal,
    val voidedOrders: BigDecimal
)

data class testReportResponse(
    val total: BigDecimal,
    val message: String,
    val date: String,
    val mengtyVoice: String?
)