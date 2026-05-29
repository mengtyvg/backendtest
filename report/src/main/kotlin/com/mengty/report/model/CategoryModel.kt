package com.mengty.report.model

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "category")
data class Category(
    @Id
    val id: UUID,

    val name: String = ""
)