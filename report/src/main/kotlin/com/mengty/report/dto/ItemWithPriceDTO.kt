package com.mengty.report.dto

import com.mengty.report.model.Category
import java.math.BigDecimal
import java.util.UUID

data class ItemWithPriceDTO(

val itemCode: String,
val itemVariantId: UUID?,
val itemName: String,
val imageUrl: String?,
val displayOrder: Int?,
val itemPrice: BigDecimal,
val stockQuantity: BigDecimal,
val mengtyName: String?,
val categoryId: UUID?

)
