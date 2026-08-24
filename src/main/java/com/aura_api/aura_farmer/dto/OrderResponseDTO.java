package com.aura_api.aura_farmer.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
public class OrderResponseDTO {
    private UUID orderId;
    private String username;
    private Long auraAmount;
    private BigDecimal amountArs;
    private String initPoint;
}