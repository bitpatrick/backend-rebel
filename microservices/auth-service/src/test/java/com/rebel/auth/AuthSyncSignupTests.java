package com.rebel.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.rebel.api.core.auth.JwtService;
import com.rebel.auth.repo.UserRepository;

@SpringBootTest
@ActiveProfiles("local")
@AutoConfigureWebTestClient
@Import(TestChannelBinderConfiguration.class)
class AuthSyncSignupTests extends MyNoSqlTestBase {

    @Autowired
    private WebTestClient client;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private StreamBridge streamBridge;

    @AfterEach
    void doesNotPublishTeacherEvents() {
        // Spring invia al bean eventi di lifecycle: verifichiamo solo la pubblicazione.
        verify(streamBridge, never()).send(anyString(), any());
    }

    @Test
    void createsUserWithHashedPasswordAndReturnsCreatedWithoutPassword() {
        String email = "sync-created@rebel.com";

        signup(email, "TEACHER", token("ADMIN"))
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isNumber()
                .jsonPath("$.email").isEqualTo(email)
                .jsonPath("$.role").isEqualTo("TEACHER")
                .jsonPath("$.password").doesNotExist();

        var saved = userRepository.findByEmail(email).block(Duration.ofSeconds(5));
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isPositive();
        assertThat(passwordEncoder.matches("password", saved.getPassword())).isTrue();
    }

    @Test
    void rejectsDuplicateEmail() {
        String email = "sync-duplicate@rebel.com";
        String admin = token("ADMIN");
        signup(email, "TEACHER", admin).expectStatus().isCreated();

        signup(email, "TEACHER", admin)
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.message").isEqualTo("User already exists");

        assertThat(userRepository.findAll().filter(user -> email.equals(user.getEmail()))
                .count().block(Duration.ofSeconds(5))).isEqualTo(1L);
    }

    @Test
    void rejectsUnknownRoleWithoutSavingUser() {
        String email = "sync-invalid-role@rebel.com";
        signup(email, "UNKNOWN", token("ADMIN"))
                .expectStatus().isNotFound()
                .expectBody().jsonPath("$.message").isEqualTo("Role not found");
        assertThat(userRepository.existsByEmail(email).block(Duration.ofSeconds(5))).isFalse();
    }

    @Test
    void requiresAdministrator() {
        String email = "sync-forbidden@rebel.com";
        signup(email, "TEACHER", token("TEACHER")).expectStatus().isForbidden();
        signup(email, "TEACHER", null).expectStatus().isUnauthorized();
        assertThat(userRepository.existsByEmail(email).block(Duration.ofSeconds(5))).isFalse();
    }

    private String token(String role) {
        return jwtService.generateToken(Map.of("email", "sync-admin@rebel.com", "roles", role));
    }

    private WebTestClient.ResponseSpec signup(String email, String role, String token) {
        var request = client.post().uri("/api/sync-signup").contentType(APPLICATION_JSON);
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        // Password è WRITE_ONLY nel DTO: il JSON esplicito include il campo in ingresso.
        return request.bodyValue("""
                {
                    "name": "Pinco",
                    "surname": "Pallino",
                    "email": "%s",
                    "password": "password",
                    "role": "%s"
                }
                """.formatted(email, role)).exchange();
    }
}
