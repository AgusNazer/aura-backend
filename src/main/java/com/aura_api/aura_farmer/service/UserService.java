package com.aura_api.aura_farmer.service;

import com.aura_api.aura_farmer.dto.UserResponseDTO;
import com.aura_api.aura_farmer.model.User;
import com.aura_api.aura_farmer.repository.UserRepository;
import com.aura_api.aura_farmer.util.AuraTierHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public User getOrCreateUser(String username, String email, String customPhrase) {
        return userRepository.findByUsername(username)
                .map(existingUser -> {
                    if (customPhrase != null && !customPhrase.isBlank()) {
                        existingUser.setCustomPhrase(customPhrase);
                    }
                    if (email != null && !email.isBlank()) {
                        existingUser.setEmail(email);
                    }
                    return userRepository.save(existingUser);
                })
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .username(username)
                            .email(email)
                            .customPhrase(customPhrase)
                            .auraPercentage(0L)
                            .build();
                    return userRepository.save(newUser);
                });
    }

    @Transactional(readOnly = true)
    public UserResponseDTO getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + username));

        return UserResponseDTO.builder()
                .username(user.getUsername())
                .auraPercentage(user.getAuraPercentage())
                .customPhrase(user.getCustomPhrase())
                .tierTitle(AuraTierHelper.getTierTitle(user.getAuraPercentage()))
                .build();
    }
}