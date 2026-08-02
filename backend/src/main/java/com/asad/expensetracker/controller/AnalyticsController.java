package com.asad.expensetracker.controller;

import com.asad.expensetracker.dto.analytics.AnalyticsSummaryResponse;
import com.asad.expensetracker.security.UserPrincipal;
import com.asad.expensetracker.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/summary")
    public ResponseEntity<AnalyticsSummaryResponse> summary(@AuthenticationPrincipal UserPrincipal principal,
                                                              @RequestParam(defaultValue = "6") int months) {
        return ResponseEntity.ok(analyticsService.getSummary(principal.getId(), months));
    }
}
