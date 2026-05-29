package com.mengty.report.dto

import java.math.BigDecimal
import java.util.UUID

data class ItemRequest(
    val itemCode: String,
    val itemName: String,
    val displayOrder: Int?,
    val itemImageUrl: String?,
    val categoryId: UUID?,
    val itemPrice: BigDecimal?,
    val openingStock: Int = 0,
    val appearanceId: UUID? = null,
    val status: Boolean = true,
    val isShowInMenu: Boolean = true
)
