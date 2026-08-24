package com.aura_api.aura_farmer.service;

import com.aura_api.aura_farmer.dto.LeaderboardUserDTO;
import com.aura_api.aura_farmer.util.AuraTierHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class LeaderboardService {

    private static final String LEADERBOARD_KEY = "aura_leaderboard";
    private final StringRedisTemplate redisTemplate;


     // Actualiza o inserta el Aura de un usuario en el ranking global.

    public void updateUserAura(String username, Long totalAura) {
        redisTemplate.opsForZSet().add(LEADERBOARD_KEY, username, totalAura.doubleValue());
    }

    // Obtiene el Top N de usuarios con más Aura (por defecto 100).

    public List<LeaderboardUserDTO> getTopUsers(int limit) {
        Set<ZSetOperations.TypedTuple<String>> rangeWithScores =
                redisTemplate.opsForZSet().reverseRangeWithScores(LEADERBOARD_KEY, 0, limit - 1);

        if (rangeWithScores == null || rangeWithScores.isEmpty()) {
            return Collections.emptyList();
        }

        List<LeaderboardUserDTO> leaderboard = new ArrayList<>();
        int rank = 1;

        for (ZSetOperations.TypedTuple<String> tuple : rangeWithScores) {
            String username = tuple.getValue();
            Long score = tuple.getScore() != null ? tuple.getScore().longValue() : 0L;

            leaderboard.add(LeaderboardUserDTO.builder()
                    .rank(rank++)
                    .username(username)
                    .auraPercentage(score)
                    .tierTitle(AuraTierHelper.getTierTitle(score))
                    .build());
        }

        return leaderboard;
    }
    // Devuelve la posición de un usuario en el ranking (1-indexed).

    public Long getUserRank(String username) {
        Long rank = redisTemplate.opsForZSet().reverseRank(LEADERBOARD_KEY, username);
        return rank != null ? rank + 1 : null;
    }
}