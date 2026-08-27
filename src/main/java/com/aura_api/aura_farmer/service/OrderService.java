package com.aura_api.aura_farmer.service;

import com.aura_api.aura_farmer.dto.CreateOrderRequestDTO;
import com.aura_api.aura_farmer.dto.OrderResponseDTO;
import com.aura_api.aura_farmer.model.Order;
import com.aura_api.aura_farmer.model.OrderStatus;
import com.aura_api.aura_farmer.model.User;
import com.aura_api.aura_farmer.repository.OrderRepository;
import com.aura_api.aura_farmer.repository.UserRepository;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.*;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private static final BigDecimal PRICE_PER_AURA_PERCENT = new BigDecimal("1000.00");

    @Value("${mercadopago.access-token}")
    private String mpAccessToken;

    @Value("${mercadopago.sandbox:false}")
    private boolean isSandbox;

    @Value("${mercadopago.back-url.success:https://aura-frontend-wine.vercel.app/}")
    private String successUrl;

    @Value("${mercadopago.back-url.failure:https://aura-frontend-wine.vercel.app/}")
    private String failureUrl;

    @Value("${mercadopago.notification-url:https://aura-backend-production-6372.up.railway.app/api/v1/webhooks/mercadopago}")
    private String notificationUrl;

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final LeaderboardService leaderboardService;

    @PostConstruct
    public void initMp() {
        MercadoPagoConfig.setAccessToken(mpAccessToken);
        log.info("Mercado Pago inicializado. Modo Sandbox activo: {}", isSandbox);
    }

    @Transactional
    public OrderResponseDTO createOrder(CreateOrderRequestDTO request) {
        User user = userService.getOrCreateUser(
                request.getUsername(),
                request.getEmail(),
                request.getCustomPhrase()
        );

        BigDecimal totalAmount = PRICE_PER_AURA_PERCENT.multiply(BigDecimal.valueOf(request.getAuraAmount()));

        Order order = Order.builder()
                .user(user)
                .auraAmount(request.getAuraAmount())
                .amountArs(totalAmount)
                .status(OrderStatus.PENDING)
                .build();

        Order savedOrder = orderRepository.save(order);

        try {
            PreferenceItemRequest item = PreferenceItemRequest.builder()
                    .id(savedOrder.getId().toString())
                    .title("Aura Farm " + savedOrder.getAuraAmount() + " Aura")
                    .quantity(1)
                    .unitPrice(totalAmount.setScale(2, java.math.RoundingMode.HALF_UP))
                    .currencyId("ARS")
                    .build();

            PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                    .success(successUrl)
                    .failure(failureUrl)
                    .pending(failureUrl)
                    .build();

            PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                    .items(List.of(item))
                    .backUrls(backUrls)
                    .notificationUrl(notificationUrl)
                    .externalReference(savedOrder.getId().toString())
                    .build();

            PreferenceClient client = new PreferenceClient();
            Preference preference = client.create(preferenceRequest);

            savedOrder.setMpPreferenceId(preference.getId());
            orderRepository.save(savedOrder);

            // Determinar el initPoint según la bandera de configuración
            String targetInitPoint = isSandbox ? preference.getSandboxInitPoint() : preference.getInitPoint();

            return OrderResponseDTO.builder()
                    .orderId(savedOrder.getId())
                    .username(user.getUsername())
                    .auraAmount(savedOrder.getAuraAmount())
                    .amountArs(savedOrder.getAmountArs())
                    .initPoint(targetInitPoint)
                    .build();

        } catch (Exception e) {
            log.error("Error al generar Checkout en Mercado Pago", e);
            throw new RuntimeException("Error al conectar con la pasarela de pagos de Mercado Pago", e);
        }
    }

    @Transactional
    public void processPaymentWebhook(String paymentId) {
        try {
            PaymentClient paymentClient = new PaymentClient();
            Payment payment = paymentClient.get(Long.valueOf(paymentId));

            if (!"approved".equalsIgnoreCase(payment.getStatus())) {
                log.info("Pago ID={} recibido con estado '{}'. No se acredita Aura.", paymentId, payment.getStatus());
                return;
            }

            UUID orderId = UUID.fromString(payment.getExternalReference());
            completeOrder(orderId, paymentId);

        } catch (Exception e) {
            log.error("Error al procesar el webhook de pago ID: {}", paymentId, e);
            throw new RuntimeException("Fallo al validar el pago con Mercado Pago", e);
        }
    }

    @Transactional
    public void completeOrder(UUID orderId, String mpPaymentId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada: " + orderId));

        if (OrderStatus.APPROVED.equals(order.getStatus())) {
            log.info("Orden ID={} ya estaba aprobada previamente.", orderId);
            return;
        }

        order.setStatus(OrderStatus.APPROVED);
        order.setMpPaymentId(mpPaymentId);
        orderRepository.save(order);

        User user = order.getUser();
        long newTotalAura = user.getAuraPercentage() + order.getAuraAmount();
        user.setAuraPercentage(newTotalAura);
        userRepository.save(user);

        leaderboardService.updateUserAura(user.getUsername(), newTotalAura);
        log.info("Aura acreditada exitosamente: @{} ahora tiene {}% de Aura", user.getUsername(), newTotalAura);
    }
}