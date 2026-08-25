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
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
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

    @Value("${mercadopago.back-url.success:https://aura-frontend-wine.vercel.app}")
    private String successUrl;

    @Value("${mercadopago.back-url.failure:https://aura-frontend-wine.vercel.app}")
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
    }

    @Transactional
    public OrderResponseDTO createOrder(CreateOrderRequestDTO request) {
        // 1. Obtener o registrar usuario
        User user = userService.getOrCreateUser(
                request.getUsername(),
                request.getEmail(),
                request.getCustomPhrase()
        );

        // 2. Calcular monto en ARS
        BigDecimal totalAmount = PRICE_PER_AURA_PERCENT.multiply(BigDecimal.valueOf(request.getAuraAmount()));

        // 3. Crear la orden pendiente en base de datos
        Order order = Order.builder()
                .user(user)
                .auraAmount(request.getAuraAmount())
                .amountArs(totalAmount)
                .status(OrderStatus.PENDING)
                .build();

        Order savedOrder = orderRepository.save(order);

        try {
            // 4. Crear la preferencia limpia en Mercado Pago
            PreferenceItemRequest item = PreferenceItemRequest.builder()
                    .id(savedOrder.getId().toString())
                    .title("Aura Farm: +" + savedOrder.getAuraAmount() + "% Aura (@" + user.getUsername() + ")")
                    .quantity(1)
                    .unitPrice(totalAmount.setScale(2, java.math.RoundingMode.HALF_UP))
                    .currencyId("ARS")
                    .build();

            PreferencePayerRequest payer = PreferencePayerRequest.builder()
//                    .email(user.getEmail())
                    .email("test_user_123456@testuser.com")
                    .build();

            PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                    .items(List.of(item))
                    .payer(payer)
                    .backUrls(PreferenceBackUrlsRequest.builder()
                            .success(successUrl)
                            .failure(failureUrl)
                            .pending(failureUrl)
                            .build())
                    .notificationUrl(notificationUrl)
                    .externalReference(savedOrder.getId().toString())
                    .build();

            PreferenceClient client = new PreferenceClient();
            Preference preference = client.create(preferenceRequest);

            // Guardar el preferenceId por auditoría
            savedOrder.setMpPreferenceId(preference.getId());
            orderRepository.save(savedOrder);

            // 5. Retornar el link real de Mercado Pago (initPoint)
            return OrderResponseDTO.builder()
                    .orderId(savedOrder.getId())
                    .username(user.getUsername())
                    .auraAmount(savedOrder.getAuraAmount())
                    .amountArs(savedOrder.getAmountArs())
//                    .initPoint(preference.getInitPoint())
                    .initPoint(preference.getSandboxInitPoint())
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

            // El externalReference contiene el UUID de nuestra Order
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

        // Idempotencia: Si ya está aprobada, no volvemos a sumar
        if (OrderStatus.APPROVED.equals(order.getStatus())) {
            log.info("Orden ID={} ya estaba aprobada previamente.", orderId);
            return;
        }

        // Marcar orden como aprobada
        order.setStatus(OrderStatus.APPROVED);
        order.setMpPaymentId(mpPaymentId);
        orderRepository.save(order);

        // Sumar Aura al usuario en PostgreSQL
        User user = order.getUser();
        long newTotalAura = user.getAuraPercentage() + order.getAuraAmount();
        user.setAuraPercentage(newTotalAura);
        userRepository.save(user);

        // Sincronizar el nuevo puntaje en Redis para el Leaderboard en tiempo real
        leaderboardService.updateUserAura(user.getUsername(), newTotalAura);
        log.info("Aura acreditada exitosamente: @{} ahora tiene {}% de Aura", user.getUsername(), newTotalAura);
    }
}