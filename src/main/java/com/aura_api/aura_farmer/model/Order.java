package com.aura_api.aura_farmer.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "aura_amount", nullable = false)
    private Long auraAmount; // Cuánto % compró en esta orden (ej: 10)

    @Column(name = "amount_ars", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountArs; // Monto pagado en ARS (ej: 10000.00)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status; // PENDING, APPROVED, REJECTED

    @Column(name = "mp_preference_id", length = 100)
    private String mpPreferenceId; // ID del checkout de Mercado Pago

    @Column(name = "mp_payment_id", unique = true, length = 100)
    private String mpPaymentId; // ID de la transacción confirmada por webhook

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}