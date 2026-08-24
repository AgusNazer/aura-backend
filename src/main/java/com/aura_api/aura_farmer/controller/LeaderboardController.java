package com.aura_api.aura_farmer.controller;

import com.aura_api.aura_farmer.dto.LeaderboardUserDTO;
import com.aura_api.aura_farmer.service.LeaderboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/leaderboard")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Ajustar al dominio del front después
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    @GetMapping
    public ResponseEntity<List<LeaderboardUserDTO>> getTopUsers(
            @RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(leaderboardService.getTopUsers(limit));
    }

    @GetMapping("/{username}/rank")
    public ResponseEntity<Long> getUserRank(@PathVariable String username) {
        Long rank = leaderboardService.getUserRank(username);
        if (rank == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(rank);
    }
    // Endpoint TEMPORAL para testing
    @PostMapping("/mock-seed")
    public ResponseEntity<String> seedMockData() {
        leaderboardService.updateUserAura("elon_musk", 1500L);
        leaderboardService.updateUserAura("pedrito", 250L);
        leaderboardService.updateUserAura("messi_goat", 99999L);
        leaderboardService.updateUserAura("npc_random", 5L);
        return ResponseEntity.ok("Usuarios mock insertados en Redis con éxito");
    }
}