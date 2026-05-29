package com.mengty.report.repository

import com.mengty.report.model.PosInvoiceDetail
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface PosInvoiceDetailRepository : JpaRepository<PosInvoiceDetail, UUID> {

    fun findByPosInvoiceIdOrderByDateCreatedAsc(posInvoiceId: UUID): List<PosInvoiceDetail>
}
