package com.asad.expensetracker.service;

import com.asad.expensetracker.dto.user.ChangePasswordRequest;
import com.asad.expensetracker.dto.user.UpdateProfileRequest;
import com.asad.expensetracker.dto.user.UserResponse;
import com.asad.expensetracker.exception.BadRequestException;
import com.asad.expensetracker.exception.DuplicateResourceException;
import com.asad.expensetracker.exception.ResourceNotFoundException;
import com.asad.expensetracker.mapper.Mappers;
import com.asad.expensetracker.model.User;
import com.asad.expensetracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserResponse getProfile(Long userId) {
        return Mappers.toUserResponse(findUser(userId));
    }

    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findUser(userId);
        String newEmail = request.email().trim().toLowerCase();

        if (!user.getEmail().equalsIgnoreCase(newEmail) && userRepository.existsByEmailIgnoreCase(newEmail)) {
            throw new DuplicateResourceException("An account with this email already exists");
        }

        user.setName(request.name().trim());
        user.setEmail(newEmail);
        return Mappers.toUserResponse(userRepository.save(user));
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = findUser(userId);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new BadRequestException("New password must be different from the current password");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        // Force re-login on other devices once the password changes.
        user.setRefreshTokenHash(null);
        user.setRefreshTokenExpiry(null);
        userRepository.save(user);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
