package com.asad.expensetracker.controller;

import com.asad.expensetracker.dto.user.ChangePasswordRequest;
import com.asad.expensetracker.dto.user.DeleteAccountRequest;
import com.asad.expensetracker.dto.user.UpdateProfileRequest;
import com.asad.expensetracker.dto.user.UserResponse;
import com.asad.expensetracker.security.UserPrincipal;
import com.asad.expensetracker.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getProfile(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userService.getProfile(principal.getId()));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserResponse> updateProfile(@AuthenticationPrincipal UserPrincipal principal,
                                                        @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(principal.getId(), request));
    }

    @PutMapping("/profile/password")
    public ResponseEntity<Map<String, String>> changePassword(@AuthenticationPrincipal UserPrincipal principal,
                                                                @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(principal.getId(), request);
        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    }

    @DeleteMapping("/profile")
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal UserPrincipal principal,
                                               @Valid @RequestBody DeleteAccountRequest request) {
        userService.deleteAccount(principal.getId(), request.password());
        return ResponseEntity.noContent().build();
    }
}
