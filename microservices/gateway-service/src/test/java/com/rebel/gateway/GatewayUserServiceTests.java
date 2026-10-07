//package com.rebel.gateway;
//
//import static com.github.tomakehurst.wiremock.client.WireMock.ok;
//import static com.github.tomakehurst.wiremock.client.WireMock.post;
//import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
//import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
//
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.Map;
//
//import org.junit.jupiter.api.BeforeAll;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.TestInstance;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.web.server.LocalServerPort;
//import org.springframework.context.annotation.Import;
//import org.springframework.http.MediaType;
//import org.springframework.test.web.reactive.server.WebTestClient;
//
//import com.github.tomakehurst.wiremock.WireMockServer;
//import com.rebel.gateway.service.JwtService;
//
//import lombok.extern.slf4j.Slf4j;
//import wiremock.org.apache.hc.core5.http.ContentType;
//
//@Slf4j
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//@SpringBootTest(classes = GatewayServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@Import(LoadBalancerTestConfiguration.class)
//class GatewayUserServiceTests {
//
//	@LocalServerPort
//	private int gatewayPort;
//
//	@Autowired
//	private WireMockServer authServiceMock;
//	
//	private WebTestClient webTestClient;
//
//	private String teacherToken;
//	private String adminToken;
//	private static final String signup = "/api/users/signup";
//	
//	@Autowired
//	private JwtService jwtService;
//
//	// =========================================================
//	// SETUP
//	// =========================================================
//	@BeforeAll
//	void setUp() throws Exception {
//		
//		// JWT
//		teacherToken = generateTokenWithRole("john_doe@email.com", "TEACHER");
//
//		adminToken = generateTokenWithRole("admin@email.com", "ADMIN");
//
//		log.info("JWT di test generati");
//
//		// Gateway
//		webTestClient = WebTestClient.bindToServer().baseUrl("http://localhost:" + gatewayPort).build();
//
//		log.info("Gateway disponibile su porta {}", gatewayPort);
//		
//	}
//	
//	// =========================================================
//	// SIGNUP - NO TOKEN
//	// =========================================================
//
//	@Test
//	void givenNoToken_whenAccessingSignup_thenReturnsUnauthorized() {
//
//		webTestClient.post().uri(signup)
//		.header("Content-Type", ContentType.APPLICATION_JSON.toString()).bodyValue("""
//				{
//				    "user": {
//				        "name": "John",
//				        "surname": "Doe",
//				        "email": "john@email.com",
//				        "password": "password",
//				        "roles": ["USER"]
//				    }
//				}
//				""").exchange().expectStatus().isUnauthorized();
//	}
//
//	// =========================================================
//	// SIGNUP - USER TOKEN
//	// =========================================================
//
//	@Test
//	void givenValidUserToken_whenAccessingSignup_thenReturnsForbidden() {
//
//		webTestClient.post().uri(signup).header("Authorization", "Bearer " + teacherToken)
//				.header("Content-Type", ContentType.APPLICATION_JSON.toString()).bodyValue("""
//						{
//						    "user": {
//						        "name": "John",
//						        "surname": "Doe",
//						        "email": "john@email.com",
//						        "password": "password",
//						        "roles": ["USER"]
//						    }
//						}
//						""").exchange().expectStatus().isForbidden();
//	}
//
////=========================================================
////SIGNUP - ADMIN TOKEN
////=========================================================
//
//	@Test
//	void givenValidAdminToken_whenAccessingSignup_thenReturnsOk() {
//
//		log.info("Testing POST /api/users/signup");
//
//		authServiceMock.stubFor(post(urlEqualTo("/api/users/signup")).willReturn(ok()));
//		
//		log.info("WireMock stub configurato per POST /api/users/signup");
//
//		webTestClient.post().uri("/api/users/signup").header("Authorization", "Bearer " + adminToken)
//		.contentType(MediaType.APPLICATION_JSON)		
//		.bodyValue("""
//						{
//						    "user": {
//						        "name": "John",
//						        "surname": "Doe",
//						        "email": "john@email.com",
//						        "password": "password",
//						        "roles": ["USER"]
//						    }
//						}
//						""")
//		.exchange().expectStatus().isOk();
//
//		log.info("Gateway ha restituito HTTP 200");
//
//		authServiceMock.verify(postRequestedFor(urlEqualTo("/api/users/signup")));
//
//		log.info("AUTH-SERVICE ha ricevuto POST /api/users/signup");
//	}
//	
//	@Test
//	void givenExpiredToken_whenAccessingSignup_thenReturnsUnauthorized() {
//		
//		String expiredToken = generateExpiredToken("admin@email.com", "ADMIN", 0);
//		
//		webTestClient.post()
//		.uri(signup)
//		.header("Authorization", "Bearear " + expiredToken)
//		.contentType(MediaType.APPLICATION_JSON)
//		.bodyValue("""
//				{
//				    "user": {
//				        "name": "John",
//				        "surname": "Doe",
//				        "email": "john@email.com",
//				        "password": "password",
//				        "roles": ["USER"]
//				    }
//				}
//				""")
//		.exchange()
//		.expectStatus()
//		.isUnauthorized();
//		
//	}
//	
//	@Test
//	void givenInvalidToken_whenAccessingSignup_thenReturnsUnauthorized() {
//		
//		String invalidToken = adminToken.substring(0, adminToken.length() - 1) + "X";
//		
//		webTestClient
//		.post()
//		.uri(signup)
//		.contentType(MediaType.APPLICATION_JSON)
//		.header("Authorization", "Bearear " + invalidToken)
//		.bodyValue("""
//				{
//				    "user": {
//				        "name": "John",
//				        "surname": "Doe",
//				        "email": "john@email.com",
//				        "password": "password",
//				        "roles": ["USER"]
//				    }
//				}
//				""")
//		.exchange()
//		.expectStatus()
//		.isUnauthorized();
//		
//	}
//
//	// =========================================================
//	// HELPER
//	// =========================================================
//
//	private String generateExpiredToken(String email, String role, long jwtExpiration) {
//		
//		Map<String, Object> claims = new HashMap<>();
//		claims.put("authorities", Arrays.asList(role));
//		
//		return jwtService.generateToken(claims, role, jwtExpiration);
//	}
//
//	private String generateTokenWithRole(String email, String role) {
//
//		Map<String, Object> claims = new HashMap<>();
//		claims.put("authorities", Arrays.asList(role));
//
//		return jwtService.generateToken(claims, email);
//	}
//}