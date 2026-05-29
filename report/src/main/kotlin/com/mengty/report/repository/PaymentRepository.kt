package com.mengty.report.repository

import com.mengty.report.model.Payment
import org.springframework.data.jpa.repository.JpaRepository

interface PaymentRepository : JpaRepository<Payment, Long> {
    fun findAllByOrderByCreatedAtDesc(): List<Payment>
}
