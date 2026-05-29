package com.mengty.report.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "pos_invoice_detail")
data class PosInvoiceDetail(

    @Id
    val id: UUID,

    @Column(name = "item_id")
    val itemId: UUID?,

    @Column(name = "unit_price")
    val unitPrice: Double,

    @Column(name = "qty")
    val qty: Double,

    @Column(name = "sub_total")
    val subTotal: Double,

    @Column(name = "discount_amount")
    val discountAmount: Double = 0.0,

    @Column(name = "discount_percentage")
    val discountPercentage: Double = 0.0,

    @Column(name = "cost")
    val cost: Double = 0.0,

    @Column(name = "total_cost")
    val totalCost: Double = 0.0,

    @Column(name = "conversion_factor")
    val conversionFactor: Double = 1.0,

    @Column(name = "printed_qty")
    val printedQty: Double = 0.0,

    @Column(name = "pos_invoice_id")
    val posInvoiceId: UUID,

    @Column(name = "date_created")
    val dateCreated: LocalDateTime? = LocalDateTime.now(),

    @Column(name = "status")
    val status: Boolean = true,

    @Column(name = "last_updated")
    val lastUpdated: LocalDateTime? = LocalDateTime.now(),

    @Column(name = "total")
    val total: Double,

    @Column(name = "post_date")
    val postDate: LocalDate? = LocalDate.now(),

    @Column(name = "is_discount_include_service_charge")
    val isDiscountIncludeServiceCharge: Boolean = false,

    @Column(name = "is_discount_include_condiment")
    val isDiscountIncludeCondiment: Boolean = false,

    @Column(name = "item_code")
    val itemCode: String? = null,

    @Column(name = "item_name")
    val itemName: String? = null,

    @Column(name = "item_variant_id")
    val itemVariantId: UUID? = null,

    @Column(name = "item_variant_name")
    val itemVariantName: String? = null,

    @Column(name = "sku")
    val sku: String = "",

    @Column(name = "is_item_bundle")
    val isItemBundle: Boolean = false,

    @Column(name = "is_item_bundle_option_detail")
    val isItemBundleOptionDetail: Boolean = false,

    @Column(name = "is_item_link_item_bundle")
    val isItemLinkItemBundle: Boolean = false
)
