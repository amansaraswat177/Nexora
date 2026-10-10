package com.shopsmart.accounts;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Demo users (one per tier + staff), addresses, preferences and orders in several statuses.
 * Runs only when shopsmart.seed.demo-data=true and the users table is empty.
 * Passwords are hashed with the app's BCrypt encoder, so they cannot live in a SQL seed file.
 */
@Component
@ConditionalOnProperty(name = "shopsmart.seed.demo-data", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    private static final String DEMO_PASSWORD = "Demo@1234";

    private final JdbcClient jdbc;
    private final AccountService accounts;

    public DemoDataSeeder(JdbcClient jdbc, AccountService accounts) {
        this.jdbc = jdbc;
        this.accounts = accounts;
    }

    @Override
    public void run(ApplicationArguments args) {
        Long existing = jdbc.sql("SELECT count(*) FROM users").query(Long.class).single();
        if (existing == 0) seedUsers();
    }

    private void seedUsers() {
        long admin = user("admin@shopsmart.test", "Admin@123", "Asha", "Admin", "PREMIUM", true);
        long basic = user("basic@shopsmart.test", DEMO_PASSWORD, "Bela", "Basic", "BASIC", false);
        long prime = user("prime@shopsmart.test", DEMO_PASSWORD, "Pooja", "Prime", "PRIME", false);
        long premium = user("premium@shopsmart.test", DEMO_PASSWORD, "Rahul", "Premium", "PREMIUM", false);
        for (long id : new long[]{admin, basic, prime, premium}) address(id);
        accounts.savePreferences(prime, new AccountService.PreferencesRequest(new BigDecimal("500"), new BigDecimal("1500"),
                "USD", "EXPRESS", "Prefers lightweight laptops", List.of("laptops"), List.of()));
        log.info("Demo users ready. Staff login: admin@shopsmart.test / Admin@123. Shoppers: basic|prime|premium@shopsmart.test / {}", DEMO_PASSWORD);
    }


    private long user(String email, String password, String first, String last, String tier, boolean staff) {
        Map<String, Object> res = accounts.register(new AccountService.RegisterRequest(email, password, first, last, "GLOBAL"), tier, staff);
        @SuppressWarnings("unchecked")
        Map<String, Object> u = (Map<String, Object>) res.get("user");
        return ((Number) u.get("id")).longValue();
    }

    private void address(long userId) {
        accounts.addAddress(userId, new AccountService.AddressRequest("Demo Customer", "+91 98765 43210", "221B Market Street", null,
                "Jalandhar", "Punjab", "144011", "IN", true));
    }

}
