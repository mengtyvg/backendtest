package com.mengty.report.repository

import com.mengty.report.model.Payment
import org.springframework.data.jpa.repository.JpaRepository

interface BakongRepository : JpaRepository<Payment, Long> {
    fun findByKhqrMd5(khqrMd5: String): Payment?
}