package com.mengty.report.repository

import com.mengty.report.model.PosInvoice
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface PosInvoiceRepositoryTest: JpaRepository<PosInvoice, UUID>