package com.shopsmart.accounts;

import com.shopsmart.kit.error.ApiException;
import com.shopsmart.kit.security.CurrentUser;
import com.shopsmart.kit.security.JwtService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AccountService {

    public record RegisterRequest(@NotBlank @Email String email,
                                  @NotBlank @Size(min = 8, max = 72) String password,
                                  @NotBlank @Size(max = 100) String firstName,
                                  @Size(max = 100) String lastName,
                                  @Size(max = 16) String region) {}

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}

    public record ProfileRequest(@NotBlank @Size(max = 100) String firstName, @Size(max = 100) String lastName,
                                 @Size(max = 16) String region) {}

    public record PreferencesRequest(BigDecimal budgetMin, BigDecimal budgetMax, String preferredCurrency,
                                     String deliveryPreference, String notes,
                                     List<String> categories, List<String> brands) {}

    public record AddressRequest(@NotBlank String fullName, String phone, @NotBlank String line1, String line2,
                                 @NotBlank String city, String state, @NotBlank String postalCode,
                                 @NotBlank @Size(min = 2, max = 2) String country, Boolean isDefault) {}

    private final JdbcClient jdbc;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AccountService(JdbcClient jdbc, PasswordEncoder encoder, JwtService jwt) {
        this.jdbc = jdbc;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    // ------------------------------------------------------------------ auth

    public Map<String, Object> register(RegisterRequest r) {
        return register(r, "BASIC", false);
    }

    /** Also used by the demo seeder to create users in a given tier / staff flag. */
    @Transactional
    public Map<String, Object> register(RegisterRequest r, String tier, boolean staff) {
        Integer exists = jdbc.sql("SELECT count(*) FROM users WHERE email = CAST(:e AS citext)").param("e", r.email().trim()).query(Integer.class).single();
        if (exists > 0) throw new ApiException(HttpStatus.CONFLICT, "EMAIL_TAKEN", "An account with this email already exists");
        long id = jdbc.sql("""
                INSERT INTO users (email, password, first_name, last_name, membership_tier, region, is_staff)
                VALUES (:e, :p, :f, :l, :t, :r, :s) RETURNING id
                """)
                .param("e", r.email().trim()).param("p", encoder.encode(r.password()))
                .param("f", r.firstName().trim()).param("l", r.lastName() == null ? "" : r.lastName().trim())
                .param("t", tier).param("r", r.region() == null || r.region().isBlank() ? "GLOBAL" : r.region().trim().toUpperCase())
                .param("s", staff)
                .query(Long.class).single();
        return tokensFor(id);
    }

    public Map<String, Object> login(LoginRequest r) {
        List<Map<String, Object>> rows = jdbc.sql("SELECT id, password, is_active AS active FROM users WHERE email = CAST(:e AS citext)")
                .param("e", r.email().trim()).query().listOfRows();
        if (rows.isEmpty() || !encoder.matches(r.password(), (String) rows.get(0).get("password"))
                || !Boolean.TRUE.equals(rows.get(0).get("active"))) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS", "Email or password is incorrect");
        }
        long id = ((Number) rows.get(0).get("id")).longValue();
        jdbc.sql("UPDATE users SET last_login = now() WHERE id = :id").param("id", id).update();
        return tokensFor(id);
    }

    public Map<String, Object> refresh(RefreshRequest r) {
        CurrentUser parsed;
        try {
            parsed = jwt.parse(r.refreshToken(), "refresh");
        } catch (RuntimeException e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "BAD_REFRESH_TOKEN", "Please log in again");
        }
        return tokensFor(parsed.id());
    }

    private Map<String, Object> tokensFor(long userId) {
        Map<String, Object> user = profile(userId);
        CurrentUser cu = new CurrentUser(userId, (String) user.get("email"), (String) user.get("tier"),
                (String) user.get("region"), Boolean.TRUE.equals(user.get("staff")));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("accessToken", jwt.createAccessToken(cu));
        out.put("refreshToken", jwt.createRefreshToken(cu));
        out.put("expiresIn", jwt.accessTtlSeconds());
        out.put("user", user);
        return out;
    }

    // --------------------------------------------------------------- profile

    public Map<String, Object> profile(long userId) {
        return jdbc.sql("""
                SELECT id, email::text AS email, first_name AS "firstName", last_name AS "lastName",
                       membership_tier AS tier, region, is_staff AS staff, date_joined AS "dateJoined"
                FROM users WHERE id = :id
                """).param("id", userId).query().listOfRows().stream().findFirst()
                .orElseThrow(() -> ApiException.notFound("User"));
    }

    public Map<String, Object> updateProfile(long userId, ProfileRequest r) {
        jdbc.sql("UPDATE users SET first_name = :f, last_name = :l, region = COALESCE(:r, region) WHERE id = :id")
                .param("f", r.firstName().trim()).param("l", r.lastName() == null ? "" : r.lastName().trim())
                .param("r", r.region() == null || r.region().isBlank() ? null : r.region().trim().toUpperCase())
                .param("id", userId).update();
        return profile(userId);
    }

    // ------------------------------------------------------------ preferences

    public Map<String, Object> preferences(long userId) {
        Map<String, Object> out = new LinkedHashMap<>();
        List<Map<String, Object>> rows = jdbc.sql("""
                SELECT budget_min AS "budgetMin", budget_max AS "budgetMax", preferred_currency AS "preferredCurrency",
                       delivery_preference AS "deliveryPreference", notes
                FROM customer_preferences WHERE user_id = :id
                """).param("id", userId).query().listOfRows();
        if (rows.isEmpty()) {
            out.put("budgetMin", null);
            out.put("budgetMax", null);
            out.put("preferredCurrency", "USD");
            out.put("deliveryPreference", "STANDARD");
            out.put("notes", "");
        } else {
            out.putAll(rows.get(0));
        }
        out.put("categories", jdbc.sql("SELECT c.slug FROM user_preferred_categories u JOIN categories c ON c.id = u.category_id WHERE u.user_id = :id ORDER BY c.slug")
                .param("id", userId).query(String.class).list());
        out.put("brands", jdbc.sql("SELECT b.slug FROM user_preferred_brands u JOIN brands b ON b.id = u.brand_id WHERE u.user_id = :id ORDER BY b.slug")
                .param("id", userId).query(String.class).list());
        return out;
    }

    @Transactional
    public Map<String, Object> savePreferences(long userId, PreferencesRequest r) {
        if (r.budgetMin() != null && r.budgetMax() != null && r.budgetMin().compareTo(r.budgetMax()) > 0) {
            throw ApiException.badRequest("Minimum budget cannot be higher than maximum budget");
        }
        String delivery = r.deliveryPreference() == null ? "STANDARD" : r.deliveryPreference().toUpperCase();
        if (!delivery.equals("STANDARD") && !delivery.equals("EXPRESS")) {
            throw ApiException.badRequest("Delivery preference must be STANDARD or EXPRESS");
        }
        jdbc.sql("""
                INSERT INTO customer_preferences (user_id, budget_min, budget_max, preferred_currency, delivery_preference, notes, updated_at)
                VALUES (:id, :min, :max, :cur, :del, :notes, now())
                ON CONFLICT (user_id) DO UPDATE SET budget_min = EXCLUDED.budget_min, budget_max = EXCLUDED.budget_max,
                  preferred_currency = EXCLUDED.preferred_currency, delivery_preference = EXCLUDED.delivery_preference,
                  notes = EXCLUDED.notes, updated_at = now()
                """)
                .param("id", userId).param("min", r.budgetMin()).param("max", r.budgetMax())
                .param("cur", r.preferredCurrency() == null || r.preferredCurrency().length() != 3 ? "USD" : r.preferredCurrency().toUpperCase())
                .param("del", delivery).param("notes", r.notes() == null ? "" : r.notes())
                .update();
        jdbc.sql("DELETE FROM user_preferred_categories WHERE user_id = :id").param("id", userId).update();
        jdbc.sql("DELETE FROM user_preferred_brands WHERE user_id = :id").param("id", userId).update();
        if (r.categories() != null) {
            for (String slug : r.categories()) {
                jdbc.sql("INSERT INTO user_preferred_categories (user_id, category_id) SELECT :id, id FROM categories WHERE slug = :s ON CONFLICT DO NOTHING")
                        .param("id", userId).param("s", slug).update();
            }
        }
        if (r.brands() != null) {
            for (String slug : r.brands()) {
                jdbc.sql("INSERT INTO user_preferred_brands (user_id, brand_id) SELECT :id, id FROM brands WHERE slug = :s ON CONFLICT DO NOTHING")
                        .param("id", userId).param("s", slug).update();
            }
        }
        return preferences(userId);
    }

    // -------------------------------------------------------------- addresses

    public List<Map<String, Object>> addresses(long userId) {
        return jdbc.sql("""
                SELECT id, full_name AS "fullName", phone, line1, line2, city, state, postal_code AS "postalCode",
                       country, is_default AS "isDefault"
                FROM addresses WHERE user_id = :id ORDER BY is_default DESC, id
                """).param("id", userId).query().listOfRows();
    }

    @Transactional
    public Map<String, Object> addAddress(long userId, AddressRequest a) {
        boolean makeDefault = Boolean.TRUE.equals(a.isDefault())
                || jdbc.sql("SELECT count(*) FROM addresses WHERE user_id = :id").param("id", userId).query(Long.class).single() == 0;
        if (makeDefault) jdbc.sql("UPDATE addresses SET is_default = FALSE WHERE user_id = :id").param("id", userId).update();
        long id = jdbc.sql("""
                INSERT INTO addresses (user_id, full_name, phone, line1, line2, city, state, postal_code, country, is_default)
                VALUES (:u, :fn, :ph, :l1, :l2, :city, :st, :pc, :co, :def) RETURNING id
                """)
                .param("u", userId).param("fn", a.fullName()).param("ph", a.phone()).param("l1", a.line1()).param("l2", a.line2())
                .param("city", a.city()).param("st", a.state()).param("pc", a.postalCode()).param("co", a.country().toUpperCase())
                .param("def", makeDefault).query(Long.class).single();
        return addresses(userId).stream().filter(m -> ((Number) m.get("id")).longValue() == id).findFirst().orElseThrow();
    }

    public void deleteAddress(long userId, long addressId) {
        jdbc.sql("DELETE FROM addresses WHERE id = :id AND user_id = :u").param("id", addressId).param("u", userId).update();
    }
}
