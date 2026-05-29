package com.mengty.report.repository

import com.mengty.report.model.PosPaymentMethod
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface PaymentMethodRepository : JpaRepository<PosPaymentMethod, UUID> {

    fun findAllByOrderByPaymentSubtypeAsc(): List<PosPaymentMethod>

    fun findByStatusTrueOrderByPaymentSubtypeAsc(): List<PosPaymentMethod>

    fun findFirstByPaymentSubtypeIgnoreCase(paymentSubtype: String): PosPaymentMethod?

    fun existsByIdAndStatusTrue(id: UUID): Boolean
}

