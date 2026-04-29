package com.example.gateway.filter;

import com.example.gateway.config.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Simple auth endpoint for development/testing.
 * Returns a signed JWT you can use in Authorization: Bearer <token> headers.
 *
 * POST /auth/login  { "username": "alice", "password": "pass", "role": "USER" }
 *
 * In production: replace with real user store + password hashing.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> body) {
        String username = body.getOrDefault("username", "guest");
        String password = body.getOrDefault("password", "");
        String role     = body.getOrDefault("role", "USER");

        // Mock validation — replace with real user lookup
        if (password.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password is required"));
        }

        String token = jwtUtil.generateToken(username, role);
        log.info("Token issued for user={} role={}", username, role);

        return ResponseEntity.ok(Map.of(
                "token", token,
                "username", username,
                "role", role,
                "type", "Bearer"
        ));
    }

    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validate(
            @RequestHeader("Authorization") String authHeader) {
        if (!authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body(Map.of("valid", false));
        }
        String token = authHeader.substring(7);
        boolean valid = jwtUtil.isTokenValid(token);
        if (!valid) {
            return ResponseEntity.status(401).body(Map.of("valid", false));
        }
        return ResponseEntity.ok(Map.of(
                "valid", true,
                "username", jwtUtil.extractUsername(token),
                "role", jwtUtil.extractRole(token)
        ));
    }
}