package com.mengty.report.dto

data class CreatePaymentMethodRequest(
    val paymentSubtype: String,
    val status: Boolean = true
)

data class PaymentMethodStatusRequest(
    val status: Boolean
)
