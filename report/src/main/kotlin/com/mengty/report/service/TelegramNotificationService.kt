package com.mengty.report.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.LocalDateTime

@Service
class TelegramNotificationService(
    @Value("\${telegram.enabled:false}")
    private val enabled: Boolean,
    @Value("\${telegram.bot-token:}")
    private val botToken: String,
    @Value("\${telegram.chat-id:}")
    private val chatId: String,
    @Value("\${telegram.base-url:https://api.telegram.org}")
    private val baseUrl: String
) {
    private val logger = LoggerFactory.getLogger(TelegramNotificationService::class.java)
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build()

    fun notifyOrderCompleted(
        series: String,
        total: BigDecimal,
        totalQty: BigDecimal,
        createdAt: LocalDateTime,
        paymentSubtype: String
    ) {
        sendMessage(
            """
            <b>Order Completed</b>
            Invoice: <code>${escapeHtml(series)}</code>
            Total: <b>${formatMoney(total)}</b>
            Items: ${escapeHtml(totalQty.stripTrailingZeros().toPlainString())}
            Payment Method: ${escapeHtml(paymentSubtype)}
            Time: ${escapeHtml(createdAt.toString().replace("T", " "))}
            """.trimIndent()
        )
    }

    fun notifyInvoiceVoided(
        series: String?,
        refundAmount: BigDecimal,
        reason: String,
        voidedBy: String?,
        voidedAt: LocalDateTime
    ) {
        sendMessage(
            """
            <b>Invoice Voided</b>
            Invoice: <code>${escapeHtml(series ?: "-")}</code>
            Refund: <b>${formatMoney(refundAmount)}</b>
            Reason: ${escapeHtml(reason)}
            Voided By: ${escapeHtml(voidedBy ?: "-")}
            Time: ${escapeHtml(voidedAt.toString().replace("T", " "))}
            """.trimIndent()
        )
    }

    private fun sendMessage(text: String) {
        if (!enabled || botToken.isBlank() || chatId.isBlank()) {
            return
        }

        try {
            val endpoint = "${baseUrl.trimEnd('/')}/bot$botToken/sendMessage"
            val request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(buildJsonBody(text)))
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() !in 200..299) {
                logger.warn("Telegram notification failed with status {}: {}", response.statusCode(), response.body())
            }
        } catch (ex: Exception) {
            logger.warn("Telegram notification failed: {}", ex.message)
        }
    }

    private fun buildJsonBody(text: String): String {
        return """
            {
              "chat_id": "${jsonEscape(chatId)}",
              "text": "${jsonEscape(text)}",
              "parse_mode": "HTML"
            }
        """.trimIndent()
    }

    private fun formatMoney(value: BigDecimal): String {
        return "$" + value.setScale(2).toPlainString()
    }

    private fun escapeHtml(value: String): String {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }

    private fun jsonEscape(value: String): String {
        return buildString {
            value.forEach { char ->
                when (char) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(char)
                }
            }
        }
    }
}
