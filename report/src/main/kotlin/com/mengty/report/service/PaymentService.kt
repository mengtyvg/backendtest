package com.mengty.report.service

import com.mengty.report.dto.BakongCheckResponse
import com.mengty.report.dto.CreatePaymentRequest;
import com.mengty.report.dto.CreatePaymentResponse
import com.mengty.report.dto.PaymentHistoryResponse
import com.mengty.report.dto.PaymentStatusResponse
import com.mengty.report.model.Payment
import com.mengty.report.repository.ItemVariantRepository
import com.mengty.report.repository.PaymentRepository;
import kh.gov.nbc.bakong_khqr.BakongKHQR
import kh.gov.nbc.bakong_khqr.model.KHQRCurrency
import kh.gov.nbc.bakong_khqr.model.MerchantInfo
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal;
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

@Service
class PaymentService(
        private val paymentRepository:PaymentRepository,
        private val itemVariantRepository: ItemVariantRepository,
        @Value("\${bakong.merchant-account}") private val merchantAccount: String,
        @Value("\${bakong.currency:USD}") private val currency: String,
        @Value("\${bakong.token}") private val bakongToken: String,
        @Value("\${bakong.base-url}") private val bakongBaseUrl: String,
        @Value("\${server.port}") private val serverPort: String,
        @Value("\${app.public-base-url:}") private val publicBaseUrl: String
) {
    private val restClient = RestClient.create()

    fun createPayment(request:CreatePaymentRequest): CreatePaymentResponse {
        val paymentItems = if (request.items.isNotEmpty()) {
            request.items
        } else {
            listOf(
                com.mengty.report.dto.CreatePaymentItemRequest(
                    itemVariantId = request.itemVariantId
                        ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Please provide at least one payment item."),
                    qty = BigDecimal.ONE
                )
            )
        }

        val variants = paymentItems.map { paymentItem ->
            if (paymentItem.qty <= BigDecimal.ZERO) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment item quantity must be greater than zero.")
            }

            val itemVariant = itemVariantRepository.findById(paymentItem.itemVariantId)
                .orElseThrow {
                    ResponseStatusException(HttpStatus.NOT_FOUND, "Item variant was not found.")
                }
            if (!itemVariant.status || itemVariant.isActive == false) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Item variant is not available.")
            }

            itemVariant to paymentItem.qty
        }

        val amount = variants.fold(BigDecimal.ZERO) { sum, (variant, qty) ->
            sum + BigDecimal.valueOf(variant.price).multiply(qty)
        }.setScale(2, RoundingMode.HALF_UP)
        val firstVariant = variants.first().first
        val purpose = if (variants.size == 1) firstVariant.name else "POS sale ${variants.size} items"

        val khqrPayment = generateKhqr(
            amount = amount,
            billNumber = "PAY-${System.currentTimeMillis()}",
            itemName = purpose
        )
        val now = LocalDateTime.now()

        val payment = Payment(
            itemVariantId = firstVariant.id,
            amount = amount,
            currency = currency,
            merchantAccount = merchantAccount,
            khqr = khqrPayment.qr,
            khqrMd5 = khqrPayment.md5,
            status = "PENDING",
            createdAt = now,
            expiresAt = now.plusMinutes(10)
        )

        val saved = paymentRepository.save(payment)

        return CreatePaymentResponse(
                paymentId = saved.id!!,
                amount = saved.amount.toPlainString(),
                currency = saved.currency,
                khqr = saved.khqr,
                md5 = saved.khqrMd5,
                status = saved.status,
                expiresAt = saved.expiresAt.toUtcInstant(),
                qrImage = khqrPayment.image,
                deeplink = khqrPayment.deeplink,
                checkoutUrl = khqrPayment.checkoutUrl,
                checkoutSessionId = khqrPayment.checkoutSessionId,
                iframeSnippet = khqrPayment.iframeSnippet
        )
    }

    fun getPaymentStatus(id: Long): PaymentStatusResponse {
        val payment = paymentRepository.findById(id)
                .orElseThrow { RuntimeException("Payment not found") }

        return PaymentStatusResponse(
                paymentId = payment.id!!,
                status = payment.status,
                bakongHash = payment.bakongHash,
                expiresAt = payment.expiresAt.toUtcInstant()
        )
    }

    fun getPaymentHistory(): List<PaymentHistoryResponse> {
        return paymentRepository.findAllByOrderByCreatedAtDesc()
            .take(100)
            .map { payment ->
                PaymentHistoryResponse(
                    paymentId = payment.id!!,
                    amount = payment.amount.toPlainString(),
                    currency = payment.currency,
                    status = payment.status,
                    bakongHash = payment.bakongHash,
                    createdAt = payment.createdAt,
                    expiresAt = payment.expiresAt.toUtcInstant(),
                    paidAt = payment.paidAt
                )
            }
    }

    fun checkPayment(id: Long): BakongCheckResponse {
        val payment = paymentRepository.findById(id)
            .orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND, "Payment was not found.")
            }

        if (payment.status == "PAID") {
            return BakongCheckResponse(
                paymentId = payment.id!!,
                status = payment.status,
                bakongHash = payment.bakongHash,
                responseCode = 0,
                responseMessage = "Already paid",
                expiresAt = payment.expiresAt.toUtcInstant()
            )
        }

        if (payment.khqrMd5.startsWith(CHECKOUT_SESSION_PREFIX)) {
            return checkWebCheckoutPayment(payment)
        }

        if (payment.status == "EXPIRED" || LocalDateTime.now().isAfter(payment.expiresAt)) {
            if (payment.status != "EXPIRED") {
                payment.status = "EXPIRED"
                paymentRepository.save(payment)
            }
            return BakongCheckResponse(
                paymentId = payment.id!!,
                status = payment.status,
                bakongHash = payment.bakongHash,
                responseCode = 2,
                responseMessage = "Payment QR has expired.",
                expiresAt = payment.expiresAt.toUtcInstant()
            )
        }

        val response = try {
            restClient.post()
                .uri("${bakongBaseUrl.trimEnd('/')}/v1/check_transaction_by_md5")
                .header("Authorization", "Bearer $bakongToken")
                .header("User-Agent", "MengtyPOS/1.0")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(mapOf("md5" to payment.khqrMd5))
                .retrieve()
                .body(BakongTransactionResponse::class.java)
                ?: return BakongCheckResponse(
                    paymentId = payment.id!!,
                    status = payment.status,
                    bakongHash = payment.bakongHash,
                    responseCode = null,
                    responseMessage = "Empty response from Bakong.",
                    expiresAt = payment.expiresAt.toUtcInstant()
                )
        } catch (ex: RestClientResponseException) {
            return BakongCheckResponse(
                paymentId = payment.id!!,
                status = payment.status,
                bakongHash = payment.bakongHash,
                responseCode = ex.statusCode.value(),
                responseMessage = "Bakong check failed with HTTP ${ex.statusCode.value()}. Check BAKONG_TOKEN and Bakong API access.",
                expiresAt = payment.expiresAt.toUtcInstant()
            )
        } catch (ex: RestClientException) {
            return BakongCheckResponse(
                paymentId = payment.id!!,
                status = payment.status,
                bakongHash = payment.bakongHash,
                responseCode = null,
                responseMessage = "Bakong check failed: ${ex.message ?: "Unable to reach Bakong."}",
                expiresAt = payment.expiresAt.toUtcInstant()
            )
        }

        if (response.responseCode == 0 && response.data?.hash != null) {
            payment.status = "PAID"
            payment.bakongHash = response.data.hash
            payment.paidAt = LocalDateTime.now()
            paymentRepository.save(payment)
        }

        return BakongCheckResponse(
            paymentId = payment.id!!,
            status = payment.status,
            bakongHash = payment.bakongHash,
            responseCode = response.responseCode,
            responseMessage = response.responseMessage,
            expiresAt = payment.expiresAt.toUtcInstant()
        )
    }

    private fun LocalDateTime.toUtcInstant(): Instant =
        atZone(ZoneOffset.UTC).toInstant()

    private fun checkWebCheckoutPayment(payment: Payment): BakongCheckResponse {
        val sessionId = payment.khqrMd5.removePrefix(CHECKOUT_SESSION_PREFIX)
        val response = try {
            restClient.post()
                .uri("${bakongBaseUrl.trimEnd('/')}/v1/web_checkouts/details")
                .header("Authorization", "Bearer $bakongToken")
                .header("User-Agent", "MengtyPOS/1.0")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(mapOf("session_id" to sessionId))
                .retrieve()
                .body(RelayCheckoutDetailsResponse::class.java)
                ?: return BakongCheckResponse(
                    paymentId = payment.id!!,
                    status = payment.status,
                    bakongHash = payment.bakongHash,
                    responseCode = null,
                    responseMessage = "Empty response from Bakong Relay checkout.",
                    expiresAt = payment.expiresAt.toUtcInstant()
                )
        } catch (ex: RestClientResponseException) {
            return BakongCheckResponse(
                paymentId = payment.id!!,
                status = payment.status,
                bakongHash = payment.bakongHash,
                responseCode = ex.statusCode.value(),
                responseMessage = "Bakong Relay checkout check failed with HTTP ${ex.statusCode.value()}.",
                expiresAt = payment.expiresAt.toUtcInstant()
            )
        } catch (ex: RestClientException) {
            return BakongCheckResponse(
                paymentId = payment.id!!,
                status = payment.status,
                bakongHash = payment.bakongHash,
                responseCode = null,
                responseMessage = "Bakong Relay checkout check failed: ${ex.message ?: "Unable to reach Bakong Relay."}",
                expiresAt = payment.expiresAt.toUtcInstant()
            )
        }

        when (response.data?.status?.uppercase()) {
            "PAID" -> {
                payment.status = "PAID"
                payment.bakongHash = response.data.data?.hash
                payment.paidAt = LocalDateTime.now()
                paymentRepository.save(payment)
            }
            "EXPIRED" -> {
                payment.status = "EXPIRED"
                paymentRepository.save(payment)
            }
        }

        return BakongCheckResponse(
            paymentId = payment.id!!,
            status = payment.status,
            bakongHash = payment.bakongHash,
            responseCode = response.responseCode,
            responseMessage = response.responseMessage,
            expiresAt = payment.expiresAt.toUtcInstant()
        )
    }

    private fun generateKhqr(
        amount: BigDecimal,
        billNumber: String,
        itemName: String
    ): KhqrPayment {
        if (bakongBaseUrl.contains("bakongrelay.com", ignoreCase = true)) {
            return createRelayWebCheckout(amount, billNumber)
        }

        val merchantInfo = MerchantInfo().apply {
            bakongAccountId = merchantAccount
            merchantName = "SOVANN MENGTY TEST BAKONG"
            merchantCity = "PHNOM PENH"
            merchantId = "MENGTY"
            acquiringBank = "Bakong"
            this.currency = KHQRCurrency.valueOf(this@PaymentService.currency.uppercase())
            this.amount = amount.toDouble()
            this.billNumber = billNumber
            storeLabel = "Report POS"
            terminalLabel = "POS-1"
            purposeOfTransaction = itemName.take(25)
            expirationTimestamp = System.currentTimeMillis() + 10 * 60 * 1000
            merchantCategoryCode = "5999"
        }

        val response = BakongKHQR.generateMerchant(merchantInfo)
        val status = response.khqrStatus
        if (status.code != 0 || response.data == null) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                status.message ?: "Could not generate KHQR."
            )
        }

        return KhqrPayment(
            qr = response.data.qr,
            md5 = response.data.md5
        )
    }

    private fun createRelayWebCheckout(
        amount: BigDecimal,
        billNumber: String
    ): KhqrPayment {
        val response = restClient.post()
            .uri("${bakongBaseUrl.trimEnd('/')}/v1/web_checkouts/create")
            .header("Authorization", "Bearer $bakongToken")
            .header("User-Agent", "MengtyPOS/1.0")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(
                mapOf(
                    "trans_id" to billNumber,
                    "req_custom" to mapOf(
                        "lang" to "en",
                        "ttl" to 10
                    ),
                    "req_khqr" to mapOf(
                        "account_id" to merchantAccount,
                        "merchant_name" to "SOVANN MENGTY TEST BAKONG",
                        "merchant_city" to "Phnom Penh",
                        "amount" to amount.toDouble(),
                        "currency" to currency.uppercase()
                    ),
                    "req_url" to mapOf(
                        "return_url" to localUrl("/sale.html"),
                        "webhook_url" to localUrl("/api/payments/webhook")
                    )
                )
            )
            .retrieve()
            .body(RelayCheckoutCreateResponse::class.java)
            ?: throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "Empty response from Bakong Relay checkout.")

        val data = response.data
        if (response.responseCode != 0 || data?.checkout_url.isNullOrBlank() || data.session_id.isNullOrBlank()) {
            throw ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                response.responseMessage ?: "Could not create Bakong Relay checkout session."
            )
        }

        return KhqrPayment(
            qr = data.checkout_url,
            md5 = CHECKOUT_SESSION_PREFIX + data.session_id,
            checkoutUrl = data.checkout_url,
            checkoutSessionId = data.session_id,
            iframeSnippet = data.iframe_snippet
        )
    }

    private fun localUrl(path: String): String =
        "${publicBaseUrl.trim().ifBlank { "http://localhost:$serverPort" }.trimEnd('/')}$path"

    private fun generateRelayKhqr(
        amount: BigDecimal,
        billNumber: String,
        itemName: String
    ): KhqrPayment {
        val payload = mapOf(
            "bank_account" to merchantAccount,
            "merchant_name" to "SOVANN MENGTY TEST BAKONG",
            "merchant_city" to "Phnom Penh",
            "amount" to amount.toDouble(),
            "currency" to currency.uppercase(),
            "store_label" to "Report POS",
            "bill_number" to billNumber.take(25),
            "terminal_label" to "POS-1",
            "static" to false,
            "expiration" to 1
        )

        val response = restClient.post()
            .uri("${bakongBaseUrl.trimEnd('/')}/v1/generate_qr")
            .header("Authorization", "Bearer $bakongToken")
            .header("User-Agent", "MengtyPOS/1.0")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(payload)
            .retrieve()
            .body(RelayKhqrResponse::class.java)
            ?: throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "Empty response from Bakong Relay.")

        if (response.responseCode != 0 || response.data?.qr.isNullOrBlank() || response.data.md5.isNullOrBlank()) {
            throw ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                response.responseMessage ?: "Could not generate KHQR through Bakong Relay."
            )
        }

        val deeplink = generateRelayDeeplink(response.data.qr)
        val image = generateRelayImage(response.data.qr) ?: response.data.image

        return KhqrPayment(
            qr = response.data.qr,
            md5 = response.data.md5,
            image = image,
            deeplink = deeplink
        )
    }

    private fun generateRelayImage(qr: String): String? {
        return try {
            val response = restClient.post()
                .uri("${bakongBaseUrl.trimEnd('/')}/v1/generate_khqr_image")
                .header("Authorization", "Bearer $bakongToken")
                .header("User-Agent", "MengtyPOS/1.0")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(mapOf("qr" to qr))
                .retrieve()
                .body(RelayKhqrResponse::class.java)

            response?.data?.image
        } catch (_: Exception) {
            null
        }
    }

    private fun generateRelayDeeplink(qr: String): String? {
        return try {
            val response = restClient.post()
                .uri("${bakongBaseUrl.trimEnd('/')}/v1/generate_deeplink_by_qr")
                .header("Authorization", "Bearer $bakongToken")
                .header("User-Agent", "MengtyPOS/1.0")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(
                    mapOf(
                        "qr" to qr,
                        "sourceInfo" to mapOf(
                            "appName" to "Mengty POS",
                            "appIconUrl" to "https://bakong.nbc.gov.kh/images/logo.svg",
                            "appDeepLinkCallback" to "http://localhost:8124/sale.html"
                        )
                    )
                )
                .retrieve()
                .body(RelayDeeplinkResponse::class.java)

            response?.data?.shortLink
        } catch (_: Exception) {
            null
        }
    }

    private data class KhqrPayment(
        val qr: String,
        val md5: String,
        val image: String? = null,
        val deeplink: String? = null,
        val checkoutUrl: String? = null,
        val checkoutSessionId: String? = null,
        val iframeSnippet: String? = null
    )

    private data class RelayCheckoutCreateResponse(
        val responseCode: Int?,
        val responseMessage: String?,
        val errorCode: Int?,
        val data: RelayCheckoutCreateData?
    )

    private data class RelayCheckoutCreateData(
        val id: String?,
        val trans_id: String?,
        val session_id: String?,
        val checkout_url: String?,
        val iframe_snippet: String?
    )

    private data class RelayCheckoutDetailsResponse(
        val responseCode: Int?,
        val responseMessage: String?,
        val errorCode: Int?,
        val data: RelayCheckoutDetailsData?
    )

    private data class RelayCheckoutDetailsData(
        val status: String?,
        val data: RelayCheckoutTransactionData?
    )

    private data class RelayCheckoutTransactionData(
        val hash: String?
    )

    private data class RelayKhqrResponse(
        val responseCode: Int?,
        val responseMessage: String?,
        val data: RelayKhqrData?
    )

    private data class RelayKhqrData(
        val qr: String?,
        val md5: String?,
        val image: String?
    )

    private data class RelayDeeplinkResponse(
        val responseCode: Int?,
        val responseMessage: String?,
        val data: RelayDeeplinkData?
    )

    private data class RelayDeeplinkData(
        val shortLink: String?
    )

    private data class BakongTransactionResponse(
        val responseCode: Int?,
        val responseMessage: String?,
        val errorCode: Int?,
        val data: BakongTransactionData?
    )

    private data class BakongTransactionData(
        val hash: String?,
        val fromAccountId: String?,
        val toAccountId: String?,
        val currency: String?,
        val amount: BigDecimal?,
        val externalRef: String?
    )

    private companion object {
        const val CHECKOUT_SESSION_PREFIX = "checkout:"
    }
}
