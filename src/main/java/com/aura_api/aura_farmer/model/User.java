package com.aura_api.aura_farmer.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_username", columnList = "username", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String username; // Ej: @agustin_dev o su alias de IG/X

    @Column(length = 150)
    private String email; // para mandarle el comprobante, revisar mas adelante.

    @Column(name = "aura_percentage", nullable = false)
    @Builder.Default
    private Long auraPercentage = 0L; // Arranca en 0% por defecto

    @Column(name = "custom_phrase", length = 120)
    private String customPhrase; // Frase opcional para flexear en la tarjeta

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}