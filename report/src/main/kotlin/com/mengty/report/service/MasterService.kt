package com.mengty.report.service
import com.mengty.report.dto.ItemRequest

import com.mengty.report.repository.ItemVariantRepository
import com.mengty.report.dto.ItemWithPriceDTO
import com.mengty.report.model.Category
import com.mengty.report.model.Item
import com.mengty.report.model.ItemVariant
import com.mengty.report.repository.CategoryRepository
import com.mengty.report.repository.ItemRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.util.UUID

@Service
class MasterService(
    private val categoryRepo: CategoryRepository,
    private val itemRepo: ItemRepository,
    private val itemVariantRepo: ItemVariantRepository
) {

    fun getAllCategories(): List<Category> {
        return categoryRepo.findAll()
    }

    fun getAllItems(): List<Item> {
        return itemRepo.findAll()
    }
    fun getItemsWithPrice(): List<ItemWithPriceDTO> {

        val result = itemRepo.getItemsWithPrice()

        return result.map {
            ItemWithPriceDTO(
                itemCode = it[0] as String,
                itemVariantId = it[1] as? UUID,
                itemName = it[2] as String,
                imageUrl = it[3] as String?,
                displayOrder = (it[4] as? Number)?.toInt(),

                itemPrice = when (val v = it[5]) {
                    is BigDecimal -> v
                    is Number -> BigDecimal.valueOf(v.toDouble())
                    else -> BigDecimal.ZERO
                },

                stockQuantity = when (val v = it[6]) {
                    is BigDecimal -> v
                    is Number -> BigDecimal.valueOf(v.toDouble())
                    else -> BigDecimal.ZERO
                },

                mengtyName = it[7] as? String ?: "-",
                categoryId = it[8] as? UUID
            )
        }
    }

    fun getItemByCode(itemCode: String): Item {
        return itemRepo.findByItemCode(itemCode)
            ?: throw RuntimeException("Item not found")
    }

    fun createItem(request: ItemRequest): Item {
        val appearanceId = request.appearanceId
            ?: itemRepo.findDefaultAppearanceId()
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "No default appearance id was found.")

        // 1. Save item first
        val item = Item(
            itemCode = request.itemCode,
            itemName = request.itemName,
            displayOrder = request.displayOrder,
            itemImageUrl = request.itemImageUrl,
            categoryId = request.categoryId,
            standardBuyingRate = request.itemPrice ?: BigDecimal.ZERO,
            standardSellingRate = request.itemPrice ?: BigDecimal.ZERO,
            openingStock = request.openingStock,
            appearanceId = appearanceId,
            status = request.status,
            isShowInMenu = request.isShowInMenu
        )

        val savedItem = itemRepo.save(item)

        // 2. Save price into item_variants
        itemVariantRepo.save(
            ItemVariant(
                itemId = savedItem.id,
                price = request.itemPrice?.toDouble() ?: 0.0,
                name = request.itemName,
                sku = request.itemCode
            )
        )

        // 3. return item
        return savedItem
    }

    fun updateItem(itemCode: String, request: ItemRequest): Item {
        val existing = getItemByCode(itemCode)

        val updated = existing.copy(
            itemName = request.itemName,
            displayOrder = request.displayOrder,
            itemImageUrl = request.itemImageUrl,
            categoryId = request.categoryId,
            standardBuyingRate = request.itemPrice ?: BigDecimal.ZERO,
            standardSellingRate = request.itemPrice ?: BigDecimal.ZERO,
            openingStock = request.openingStock,
            appearanceId = request.appearanceId ?: existing.appearanceId,
            status = request.status,
            isShowInMenu = request.isShowInMenu
        )

        val savedItem = itemRepo.save(updated)

        val variant = itemVariantRepo.findFirstByItemId(savedItem.id)

        if (variant != null) {
            val updatedVariant = variant.copy(
                price = request.itemPrice?.toDouble() ?: 0.0,
                name = request.itemName,
                sku = request.itemCode
            )

            itemVariantRepo.save(updatedVariant)
        } else {
            itemVariantRepo.save(
                ItemVariant(
                    itemId = savedItem.id,
                    price = request.itemPrice?.toDouble() ?: 0.0,
                    name = request.itemName,
                    sku = request.itemCode
                )
            )
        }

        return savedItem
    }

    fun deactivateItem(itemCode: String): Item {
        val existing = getItemByCode(itemCode)

        val updated = existing.copy(status = false)

        return itemRepo.save(updated)
    }

    fun deleteItem(itemCode: String) {
        val existing = getItemByCode(itemCode)
        itemRepo.delete(existing)
    }




}
