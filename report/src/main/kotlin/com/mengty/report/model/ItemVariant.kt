package com.mengty.report.model


import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "item_variants")
data class ItemVariant(

    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "item_id")
    val itemId: UUID,

    @Column(name = "price")
    val price: Double,

    @Column(name = "cost")
    val cost: Double = 0.0,

    @Column(name = "name")
    val name: String,

    @Column(name = "sku")
    val sku: String,

    @Column(name = "status")
    val status: Boolean = true,

    @Column(name = "sync_status")
    val syncStatus: Boolean = false,

    @Column(name = "out_of_stock_status")
    val outOfStockStatus: Boolean = false,

    @Column(name = "is_recommended")
    val isRecommended: Boolean = false,

    @Column(name = "is_active")
    val isActive: Boolean? = true,

    @Column(name = "is_default")
    val isDefault: Boolean? = true,

    @Column(name = "date_created")
    val dateCreated: LocalDateTime? = LocalDateTime.now(),

    @Column(name = "last_updated")
    val lastUpdated: LocalDateTime? = LocalDateTime.now()
)