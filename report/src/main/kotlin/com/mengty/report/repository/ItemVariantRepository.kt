package com.mengty.report.repository

import com.mengty.report.model.ItemVariant
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface ItemVariantRepository : JpaRepository<ItemVariant, UUID> {

    fun findFirstByItemId(itemId: UUID): ItemVariant?

    @Query(
        """
        SELECT v
        FROM ItemVariant v
        WHERE v.itemId = :itemId
          AND v.status = true
          AND COALESCE(v.isActive, true) = true
        ORDER BY v.price DESC
        LIMIT 1
        """
    )
    fun findFirstActiveSaleVariantByItemId(@Param("itemId") itemId: UUID): ItemVariant?
}
