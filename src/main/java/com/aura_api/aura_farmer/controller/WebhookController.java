package com.aura_api.aura_farmer.controller;

import com.aura_api.aura_farmer.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final OrderService orderService;

    @PostMapping("/mercadopago")
    public ResponseEntity<Void> handleMercadoPagoNotification(
            @RequestParam(value = "data.id", required = false) String dataId,
            @RequestParam(value = "type", required = false) String type,
            @RequestBody(required = false) Map<String, Object> body) {

        String paymentId = dataId;

        // Fallback por si MP envía el ID en el body JSON
        if (paymentId == null && body != null && body.containsKey("data")) {
            Map<?, ?> data = (Map<?, ?>) body.get("data");
            if (data != null && data.get("id") != null) {
                paymentId = data.get("id").toString();
            }
        }

        if (paymentId != null && ("payment".equals(type) || (body != null && "payment".equals(body.get("type"))))) {
            log.info("Webhook recibido de Mercado Pago. Payment ID: {}", paymentId);
            orderService.processPaymentWebhook(paymentId);
        }

        // Siempre responder 200 OK a MP para confirmar recepción
        return ResponseEntity.ok().build();
    }
}