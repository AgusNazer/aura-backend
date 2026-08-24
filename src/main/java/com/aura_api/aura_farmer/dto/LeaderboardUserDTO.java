package com.aura_api.aura_farmer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardUserDTO {
    private Integer rank;
    private String username;
    private Long auraPercentage;
    private String tierTitle;
}