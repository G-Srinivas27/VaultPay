package com.srinivas.vaultpay.common.controller;

import com.srinivas.vaultpay.common.response.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * A simple health check controller.
 *
 * <p>In production, teams use Spring Boot Actuator (a separate dependency)
 * for rich health checks — database connectivity, disk space, custom metrics, etc.
 * We'll add that later. For now this gives us a quick way to verify the app is alive.
 *
 * <p>This endpoint is intentionally lightweight — it should ALWAYS respond quickly.
 * If a health check itself is slow, monitoring tools will falsely report the app as down.
 *
 * <p>Endpoint: GET /
 */
@RestController
public class HealthController {

    @Value("${spring.application.name}")
    private String appName;

    /**
     * GET /
     * Returns application status and basic info.
     * This is the first URL anyone tries when they want to verify the service is alive.
     */
    @GetMapping("/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        Map<String, Object> info = Map.of(
                "application", appName,
                "status", "UP",
                "message", "VaultPay is running!",
                "timestamp", LocalDateTime.now().toString()
        );
        return ResponseEntity.ok(ApiResponse.success("Service is healthy", info));
    }
}
