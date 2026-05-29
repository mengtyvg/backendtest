package com.mengty.report.repository

import com.mengty.report.model.PosInvoice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.UUID

@Repository
interface PosInvoiceRepository : JpaRepository<PosInvoice, UUID> {


    @Query(
        value = """
    SELECT *
    FROM pos_invoice p
    WHERE p.post_date BETWEEN :startDate AND :endDate
    ORDER BY p.date_created ASC
""",
        nativeQuery = true
    )
    fun findSaleSummaryInvoices(
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate
    ): List<PosInvoice>


    @Query(
        value = """
        SELECT *
        FROM pos_invoice p
        WHERE p.pos_order_status IN ('Completed', 'Voided')
        AND p.post_date = :date
        ORDER BY p.date_created DESC
    """,
        nativeQuery = true
    )
    fun findReportInvoicesByDate(
        @Param("date") date: LocalDate
    ): List<PosInvoice>

    @Query(
        value = """
        SELECT COALESCE(MAX(CAST(SUBSTRING(series FROM 4) AS INTEGER)), 0)
        FROM pos_invoice
        WHERE series ~ '^INV[0-9]+$'
    """,
        nativeQuery = true
    )
    fun findLastInvoiceNumber(): Int
}
