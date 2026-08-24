package com.aura_api.aura_farmer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO {
    private String username;
    private Long auraPercentage;
    private String customPhrase;
    private String tierTitle;
}