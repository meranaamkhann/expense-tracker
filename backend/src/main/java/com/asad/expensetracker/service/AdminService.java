package com.asad.expensetracker.service;

import com.asad.expensetracker.dto.admin.AdminStatsResponse;
import com.asad.expensetracker.dto.admin.AdminUserResponse;
import com.asad.expensetracker.exception.BadRequestException;
import com.asad.expensetracker.exception.ResourceNotFoundException;
import com.asad.expensetracker.model.Expense;
import com.asad.expensetracker.model.User;
import com.asad.expensetracker.repository.ExpenseRepository;
import com.asad.expensetracker.repository.RefreshTokenRepository;
import com.asad.expensetracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional(readOnly = true)
    public Page<AdminUserResponse> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toAdminUserResponse);
    }

    @Transactional
    public AdminUserResponse setUserEnabled(Long requestingAdminId, Long targetUserId, boolean enabled) {
        if (requestingAdminId.equals(targetUserId) && !enabled) {
            throw new BadRequestException("You can't disable your own admin account");
        }

        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + targetUserId));

        user.setEnabled(enabled);
        if (!enabled) {
            // Kick every active session immediately across all of this user's devices.
            refreshTokenRepository.revokeAllForUser(user.getId());
        }
        return toAdminUserResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public AdminStatsResponse getStats() {
        long totalUsers = userRepository.count();
        long totalExpenseEntries = expenseRepository.count();
        var totalExpenseVolume = expenseRepository.sumAllAmountByKind(Expense.TransactionKind.EXPENSE);
        var totalIncomeVolume = expenseRepository.sumAllAmountByKind(Expense.TransactionKind.INCOME);
        return new AdminStatsResponse(totalUsers, totalExpenseEntries, totalExpenseVolume, totalIncomeVolume);
    }

    private AdminUserResponse toAdminUserResponse(User user) {
        return new AdminUserResponse(
                user.getId(), user.getName(), user.getEmail(), user.getRole().name(),
                user.isEnabled(), user.isEmailVerified(), user.getCreatedAt());
    }
}
