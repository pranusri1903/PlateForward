package com.plateforward;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Starts the whole application against a real, throwaway PostgreSQL (Flyway migrations included) and talks to it over HTTP,
 * exactly like the frontend does. One database container is shared by every integration test class in a run.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class IntegrationTestBase {
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    static {
        POSTGRES.start(); // stopped automatically when the test JVM exits
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    record Resp(int status, String body) {
        <T> T json(String path) {
            return JsonPath.read(body, path);
        }

        long id(String path) {
            return this.<Number>json(path).longValue();
        }
    }

    record Account(long id, String token) {}

    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    RestClient http;

    @BeforeEach
    void client() {
        http = RestClient.create("http://localhost:" + port);
    }

    Resp call(HttpMethod method, String path, String token, Object body) {
        var request = http.method(method).uri(path);
        if (token != null) request.headers(h -> h.setBearerAuth(token));
        if (body != null) request.contentType(MediaType.APPLICATION_JSON).body(body);
        return request.exchange((req, res) -> new Resp(res.getStatusCode().value(), new String(res.getBody().readAllBytes(), UTF_8)));
    }

    Resp get(String path, String token) {
        return call(HttpMethod.GET, path, token, null);
    }

    Resp post(String path, String token, Object body) {
        return call(HttpMethod.POST, path, token, body);
    }

    Account register(String role) {
        var email = role.toLowerCase() + "-" + UUID.randomUUID() + "@test.com";
        var r = post("/api/auth/register", null, Map.of("email", email, "password", "password1", "name", role + " Test", "role", role, "area", "Downtown", "phone", "555-0100"));
        if (r.status() != 201) throw new AssertionError("register failed: " + r);
        return new Account(r.id("$.user.id"), r.json("$.token"));
    }

    String adminToken() {
        return post("/api/auth/login", null, Map.of("email", "admin@plateforward.local", "password", "admin12345")).json("$.token");
    }

    /** An organization that an admin has already verified, so it may claim. */
    Account verifiedOrg() {
        var org = register("ORG");
        var verified = post("/api/admin/organizations/" + org.id() + "/verify", adminToken(), null);
        if (verified.status() != 200) throw new AssertionError("verify failed: " + verified);
        return org;
    }

    /** Creates a listing whose pickup window opens, closes and whose food expires the given number of seconds from now. */
    long listing(String donorToken, String title, String audience, long startsIn, long endsIn, long expiresIn) {
        var now = Instant.now();
        var r = post("/api/listings", donorToken, Map.ofEntries(Map.entry("title", title), Map.entry("category", "BAKERY"), Map.entry("storage", "AMBIENT"),
                Map.entry("quantity", "5 loaves"), Map.entry("area", "Downtown"), Map.entry("address", "1 Main St"), Map.entry("audience", audience),
                Map.entry("pickupStart", now.plusSeconds(startsIn).toString()), Map.entry("pickupEnd", now.plusSeconds(endsIn).toString()), Map.entry("expiresAt", now.plusSeconds(expiresIn).toString())));
        if (r.status() != 201) throw new AssertionError("create listing failed: " + r);
        return r.id("$.id");
    }

    long listing(String donorToken, String title) {
        return listing(donorToken, title, "ORGS", Duration.ofHours(1).toSeconds(), Duration.ofHours(3).toSeconds(), Duration.ofHours(6).toSeconds());
    }

    String status(long listingId, String token) {
        return get("/api/listings/" + listingId, token).json("$.status");
    }
}
