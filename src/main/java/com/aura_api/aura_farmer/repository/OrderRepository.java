package com.aura_api.aura_farmer.repository;

import com.aura_api.aura_farmer.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    Optional<Order> findByMpPreferenceId(String mpPreferenceId);
    Optional<Order> findByMpPaymentId(String mpPaymentId);
}