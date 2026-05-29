package com.mengty.report.service

import com.mengty.report.dto.PosInvoiceDTO
import com.mengty.report.repository.PosInvoiceRepository
import org.springframework.stereotype.Service

@Service
class PosInvoiceService(
    private val posInvoiceRepository: PosInvoiceRepository
){
    fun mengty() = posInvoiceRepository.findAll().map {
        PosInvoiceDTO(
            it.series,
            it.posOrderStatus,
            it.total,
            it.totalQty,
            it.postDate
        )
    }
}
