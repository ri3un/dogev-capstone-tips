package com.gaebalhagae.report.controller;

import com.gaebalhagae.global.response.ApiResponse;
import com.gaebalhagae.global.security.LoginMember;
import com.gaebalhagae.report.dto.ReportData;
import com.gaebalhagae.report.service.ReportService;
import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final Clock clock;

    @GetMapping("/api/dogs/{dogId}/reports/preview")
    public ApiResponse<ReportData> preview(
            @AuthenticationPrincipal LoginMember me, @PathVariable Long dogId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(reportService.preview(me.id(), dogId, from, to));
    }

    /** PDF 파일 다운로드 (병원 제출용) */
    @GetMapping("/api/dogs/{dogId}/reports/pdf")
    public ResponseEntity<byte[]> pdf(
            @AuthenticationPrincipal LoginMember me, @PathVariable Long dogId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        byte[] pdf = reportService.pdf(me.id(), dogId, from, to);
        String filename = "kungkung-report-" + LocalDate.now(clock) + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build()
                        .toString())
                .body(pdf);
    }
}
