package com.mengty.report.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.util.UUID

@Entity
@Table(name = "item")
data class Item(

    @Id
    @Column(name = "id")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "item_code")
    val itemCode: String,

    @Column(name = "item_name")
    val itemName: String? = null,

    @Column(name = "display_order")
    val displayOrder: Int? = null,

    @Column(name = "item_image_url")
    val itemImageUrl: String? = null,

    @Column(name = "category_id")
    val categoryId: UUID? = null,

    @Column(name = "standard_buying_rate")
    val standardBuyingRate: BigDecimal = BigDecimal.ZERO,

    @Column(name = "standard_selling_rate")
    val standardSellingRate: BigDecimal = BigDecimal.ZERO,

    @Column(name = "opening_stock")
    val openingStock: Int = 0,

    @Column(name = "appearance_id")
    val appearanceId: UUID,

    @Column(name = "status")
    val status: Boolean = true,

    @Column(name = "is_show_in_menu")
    val isShowInMenu: Boolean = true
)
