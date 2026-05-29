package com.mengty.report.controller

import com.mengty.report.dto.ReportResponse
import com.mengty.report.dto.SaleSummaryResponse
import com.mengty.report.dto.testReportResponse
import com.mengty.report.service.ReportService
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = ["*"])
class ReportController(
    private val reportService: ReportService
) {

    @GetMapping("/test")
    fun getTestReport(): testReportResponse {
        val selectedDate = LocalDate.now()
        val report = reportService.getReportByDate(selectedDate)


        return testReportResponse(
            total = report.totalAmount,
            message = "test custom dto worked",
            date = selectedDate.toString(),
            mengtyVoice = report.invoices.firstOrNull()?.series
        )
    }

    @GetMapping
    fun getReport(@RequestParam date: String): ReportResponse {
        val selectedDate = LocalDate.parse(date)
        return reportService.getReportByDate(selectedDate)
    }

    @GetMapping("/sale-summary")
    fun getSalesSummary(
        @RequestParam startDate: String,
        @RequestParam endDate: String,
        @RequestParam periodType: String,
        @RequestParam shift: String
    ): SaleSummaryResponse {
        return reportService.getSaleSummary(
            LocalDate.parse(startDate),
            LocalDate.parse(endDate),
            periodType,
            shift
        )
    }


}