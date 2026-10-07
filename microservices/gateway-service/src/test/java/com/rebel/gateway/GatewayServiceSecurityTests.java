//package com.rebel.gateway;
//
//import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
//import static com.github.tomakehurst.wiremock.client.WireMock.get;
//import static com.github.tomakehurst.wiremock.client.WireMock.post;
//import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
//
//import java.lang.reflect.Method;
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.Map;
//
//import org.junit.jupiter.api.AfterAll;
//import org.junit.jupiter.api.BeforeAll;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.TestInstance;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.web.server.LocalServerPort;
//import org.springframework.cloud.gateway.route.RouteLocator;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.MediaType;
//import org.springframework.test.web.reactive.server.WebTestClient;
//import org.springframework.web.bind.annotation.PostMapping;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.github.tomakehurst.wiremock.WireMockServer;
//import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
//import com.rebel.api.core.auth.AuthController;
//import com.rebel.api.core.auth.CreateUserRequest;
//import com.rebel.api.core.auth.LoginResponse;
//import com.rebel.api.core.auth.LoginUser;
//import com.rebel.gateway.service.JwtService;
//
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//@SpringBootTest(classes = GatewayServiceApplication.class,
//		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//class GatewayServiceSecurityTests {
//
//	@Autowired
//	ObjectMapper objectMapper;
//	
//	@LocalServerPort
//	private int port;
//
//	private WebTestClient webTestClient;
//	private WireMockServer authServiceMock;
//	private WireMockServer teachServiceMock;
//
//	private String validToken;
//	private String adminToken;
//
//	@Autowired
//	private JwtService jwtService;
//	
//	@Value("${gateway.auth-path}")
//	private String authBaseUrl;
//
//	@Value("${gateway.teach-path}")
//	private String teachBaseUrl;
//	
//	private String loginUrl;
//	private String signupUrl;
//	
//	@BeforeAll
//    void setupUrls() throws Exception {
//
//        // Get the controller base path.
//        Method registerMethod =
//                AuthController.class.getMethod("register", CreateUserRequest.class);
//
//        PostMapping registerMapping =
//                registerMethod.getAnnotation(PostMapping.class);
//        
//        signupUrl = authBaseUrl + registerMapping.value()[0];
//
//        // Get the signup path.
//        Method loginMethod =
//                AuthController.class.getMethod(
//                        "authenticate",
//                        LoginUser.class
//                );
//
//        PostMapping loginMapping =
//                loginMethod.getAnnotation(PostMapping.class);
//
//        loginUrl = authBaseUrl + loginMapping.value()[0];
//	
//	}
//
//	@BeforeAll
//	void setUp() {
//
//		validToken = jwtService.generateToken("john_doe@email.com");
//		adminToken = generateTokenWithRole("admin@email.com", "ADMIN");
//
//		authServiceMock = new WireMockServer(WireMockConfiguration.wireMockConfig().port(8081));
//		authServiceMock.start();
//
//		teachServiceMock = new WireMockServer(WireMockConfiguration.wireMockConfig().port(8082));
//		teachServiceMock.start();
//
//		webTestClient = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
//	}
//
//	@Autowired
//	RouteLocator routeLocator;
//
//	@Test
//	void printRoutes() {
//		System.err.println("routes");
//	    routeLocator.getRoutes()
//	            .doOnNext(route -> {
//	                System.err.println("=================================");
//	                System.err.println("Route ID : " + route.getId());
//	                System.err.println("URI      : " + route.getUri());
//	                System.err.println("Order    : " + route.getOrder());
//	                System.err.println("Predicate: " + route.getPredicate());
//	                System.err.println("=================================");
//	            })
//	            .blockLast();
//	}
//	
//	@Test
//	void testUnauthorizedAccessToProtectedEndpoints() {
//		webTestClient.get().uri(signupUrl).exchange().expectStatus().isUnauthorized();
//		webTestClient.get().uri(teachBaseUrl + "/hello").exchange().expectStatus().isUnauthorized();
//	}
//	
//	@Test
//	void testAccessToUnprotectedEndpoints() throws JsonProcessingException {
//		
//		// given
//		String loginUser = objectMapper.writeValueAsString(new LoginUser("mario.rossi@gmail.com", "password"));
//		String loginResponse = objectMapper.writeValueAsString(new LoginResponse("token", 1));
//		
//		authServiceMock.stubFor(post(urlEqualTo(loginUrl))
//				.willReturn(aResponse()
//						.withStatus(200)
//						.withHeader("Content-Type", MediaType.APPLICATION_JSON.toString())
//						.withBody(loginResponse))
//				);
//		
//		// when & then
//	    webTestClient
//	            .post()
//	            .uri(loginUrl)
//	            .contentType(MediaType.APPLICATION_JSON)
//	            .bodyValue(loginUser)
//	            .exchange()
//	            .expectStatus().isOk();
//	    
//	}
//
//	@Test
//	void testAuthorizedAccessToProtectedEndpoint() {
//
//		teachServiceMock.stubFor(get(urlEqualTo("/api/teachers/hello"))
//				.willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
//						.withBody("{\"message\": \"Hello from teach-service!\"}")));
//
//		webTestClient.get().uri("/api/teachers/hello").header("Authorization", "Bearer " + validToken).exchange()
//				.expectStatus().isOk();
//
//	}
//
//	@Test
//	void testAccessWithInvalidToken() {
//
//		String invalidToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqb2huX2RvZUBleGFtcGxlLmNvbSJ9.X7k9mP2qL8vN4sR1tY6wE3";
//
//		webTestClient.get().uri("/api/teachers/hello").header("Authorization", "Bearer " + invalidToken).exchange()
//				.expectStatus().isUnauthorized();
//
//	}
//
//	// ============== Tests per /api/auth/hello ==============
//
//	@Test
//	void testAuthHelloWithValidToken() {
//		// Accesso autenticato a /api/auth/hello deve essere allowed
//		authServiceMock.stubFor(get(urlEqualTo("/api/auth/hello"))
//				.willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
//						.withBody("{\"message\": \"Hello from auth-service!\"}")));
//
//		webTestClient.get().uri("/api/auth/hello").header("Authorization", "Bearer " + validToken).exchange()
//				.expectStatus().isOk();
//	}
//
//	// ============== Tests per /api/auth/signup ==============
//
//	@Test
//	void testAuthSignupWithoutAdminRole() {
//		// Accesso autenticato ma senza ruolo ADMIN deve essere 403 (Forbidden)
//		webTestClient.post().uri("/api/auth/signup").header("Authorization", "Bearer " + validToken).exchange()
//				.expectStatus().isForbidden();
//	}
//
//	@Test
//	void testAuthSignupWithAdminRole() {
//		// Accesso autenticato con ruolo ADMIN deve essere allowed
//		authServiceMock.stubFor(post(urlEqualTo("/api/auth/signup"))
//				.willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
//						.withBody("{\"message\": \"User created successfully\"}")));
//
//		webTestClient.post().uri("/api/auth/signup").header("Authorization", "Bearer " + adminToken).exchange()
//				.expectStatus().isOk();
//	}
//	
//	@Test
//	void givenValidAdminToken_whenSignup_thenReturnsOk() {
//
//	    authServiceMock.stubFor(
//	            post(urlEqualTo("/api/auth/signup"))
//	                    .willReturn(
//	                            aResponse()
//	                                    .withStatus(HttpStatus.CREATED.value())
//	                                    .withHeader(
//	                                            "Content-Type",
//	                                            "application/json"
//	                                    )
//	                                    .withBody(
//	                                            "{\"message\":\"User created successfully\"}"
//	                                    )
//	                    )
//	    );
//
//	    webTestClient
//	            .post()
//	            .uri("/api/auth/signup")
//	            .header(
//	                    "Authorization",
//	                    "Bearer " + adminToken
//	            )
//	            .exchange()
//	            .expectStatus()
//	            .isCreated();
//	}
//
//	@AfterAll
//	void tearDown() {
//
//		authServiceMock.stop();
//		teachServiceMock.stop();
//	}
//
//	// ============== Helper Methods ==============
//
//	private String generateTokenWithRole(String email, String role) {
//		Map<String, Object> claims = new HashMap<>();
//		claims.put("authorities", Arrays.asList(role));
//		return jwtService.generateToken(claims, email);
//	}
//
//}