package com.mengty.report.repository

import com.mengty.report.model.Item
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ItemRepository : JpaRepository<Item, UUID> {

    fun countByCategoryId(categoryId: UUID): Long

    fun findByItemCode(itemCode: String): Item?

    fun findByItemCodeAndStatusTrue(itemCode: String): Item?

    @Query(
        value = """
        SELECT appearance_id
        FROM item
        WHERE appearance_id IS NOT NULL
        LIMIT 1
    """,
        nativeQuery = true
    )
    fun findDefaultAppearanceId(): UUID?


    @Query(
        value = """
        SELECT 
             i.item_code,
    (ARRAY_AGG(v.id ORDER BY v.price DESC))[1] AS item_variant_id,
    i.item_name,
    i.item_image_url,
    i.display_order,
    COALESCE(MAX(v.price), 0) AS item_price,
    COALESCE(MAX(b.quantity), 0) AS stock_quantity,
    c.name AS category_name,
    i.category_id
        FROM item i
        LEFT JOIN category c 
            ON i.category_id = c.id
        JOIN item_variants v 
            ON v.item_id = i.id
            AND v.status = true
            AND COALESCE(v.is_active, true) = true
        LEFT JOIN inventory_stock_balance b
            ON b.item_variant_id = v.id
        WHERE i.is_show_in_menu = true
          AND COALESCE(i.status, true) = true
        GROUP BY i.id, i.item_code, i.item_name, i.item_image_url, i.display_order, c.name, i.category_id
        ORDER BY c.name ASC, i.display_order ASC, i.item_name ASC
    """,
        nativeQuery = true
    )
    fun getItemsWithPrice(): List<Array<Any?>>


}
