package com.asad.expensetracker.controller;

import com.asad.expensetracker.dto.admin.AdminStatsResponse;
import com.asad.expensetracker.dto.admin.AdminUserResponse;
import com.asad.expensetracker.dto.admin.UpdateUserStatusRequest;
import com.asad.expensetracker.security.UserPrincipal;
import com.asad.expensetracker.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Everything under /api/admin requires ROLE_ADMIN — enforced centrally in SecurityConfig,
 * not per-method here, so there's exactly one place that can get that rule wrong.
 * See AdminBootstrapRunner for how an account becomes an admin in the first place.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    public ResponseEntity<Page<AdminUserResponse>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("id").ascending());
        return ResponseEntity.ok(adminService.listUsers(pageable));
    }

    @PutMapping("/users/{id}/status")
    public ResponseEntity<AdminUserResponse> updateUserStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserStatusRequest request) {
        return ResponseEntity.ok(adminService.setUserEnabled(principal.getId(), id, request.enabled()));
    }

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsResponse> stats() {
        return ResponseEntity.ok(adminService.getStats());
    }
}
