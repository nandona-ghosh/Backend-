package com.foodplant.api;

import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Ephemeral credentials for isolated test application contexts only. */
abstract class TestSecurityCredentials {
    private static final SecureRandom RANDOM = new SecureRandom();
    static final String TEST_PASSWORD = randomValue();
    private static final String SIGNING_KEY = randomValue();
    private static final String PASSWORD_HASH = new BCryptPasswordEncoder().encode(TEST_PASSWORD);

    private static String randomValue() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    @DynamicPropertySource
    static void testSecurityProperties(DynamicPropertyRegistry registry) {
        registry.add("plant.security.secret", () -> SIGNING_KEY);
        registry.add("plant.security.admin-hash", () -> PASSWORD_HASH);
        registry.add("plant.security.staff-hash", () -> PASSWORD_HASH);
        registry.add("plant.security.user-hash", () -> PASSWORD_HASH);
    }
}
