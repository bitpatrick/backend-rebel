//package com.rebel.gateway;
//
//import static com.github.tomakehurst.wiremock.client.WireMock.ok;
//import static com.github.tomakehurst.wiremock.client.WireMock.post;
//import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
//import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
//import static org.assertj.core.api.Assertions.assertThat;
//
//import java.net.URI;
//import java.net.http.HttpClient;
//import java.net.http.HttpRequest;
//import java.net.http.HttpResponse;
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
//import org.springframework.test.context.DynamicPropertyRegistry;
//import org.springframework.test.context.DynamicPropertySource;
//import org.springframework.test.web.reactive.server.WebTestClient;
//import org.testcontainers.containers.GenericContainer;
//import org.testcontainers.junit.jupiter.Container;
//import org.testcontainers.junit.jupiter.Testcontainers;
//import org.testcontainers.utility.DockerImageName;
//
//import com.github.tomakehurst.wiremock.WireMockServer;
//import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
//import com.netflix.appinfo.InstanceInfo;
//import com.netflix.discovery.EurekaClient;
//import com.rebel.gateway.service.JwtService;
//
//import lombok.extern.slf4j.Slf4j;
//
//@Slf4j
//@Testcontainers
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//@SpringBootTest(classes = GatewayServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
////@WireMockTest(httpPort = 8081)
//class GatewayAuthServiceTests {
//
//	@LocalServerPort
//	private int gatewayPort;
//
//	@Container
//	static GenericContainer<?> eureka = new GenericContainer<>(DockerImageName.parse("rebel/discovery-service:test"))
//			.withExposedPorts(8761);
//	
//	static WireMockServer authServiceMock;
//	
//	static EurekaTestClient eurekaTestClient;
//	
//	static {
//		eureka.start();
//		
//		// Avvio WireMock
//		authServiceMock = new WireMockServer(
//	            WireMockConfiguration.wireMockConfig()
//	                    .dynamicPort()
//	    );
//		
//		authServiceMock.start();
//		
//		log.info("WireMock AUTH-SERVICE avviato su localhost:{}", authServiceMock.port());
//		
//		log.info("========== SETUP GATEWAY TEST ==========");
//
//		// Eureka
//		String eurekaUrl = "http://" + eureka.getHost() + ":" + eureka.getMappedPort(8761) + "/eureka";
//
//		log.info("Eureka URL: {}", eurekaUrl);
//
//		eurekaTestClient = new EurekaTestClient(eurekaUrl, "admin", "admin");
//
//		// Registrazione
//		try {
//			eurekaTestClient.register(
//					"AUTH-SERVICE", 
//					"auth-service-test", 
//					"localhost", 
//					authServiceMock.port()
//					);
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//		log.info("AUTH-SERVICE registrato su localhost:{}", authServiceMock.port());
//		
//	}
//
//	private WebTestClient webTestClient;
//
//	private String teacherToken;
//	private String adminToken;
//
//	@Autowired
//	private JwtService jwtService;
//
//	// =========================================================
//	// CONFIGURAZIONE EUREKA DEL GATEWAY
//	// =========================================================
//	@DynamicPropertySource
//	static void configureEureka(DynamicPropertyRegistry registry) {
//
//		String eurekaUrl =
//		        "http://admin:admin@"
//		        + eureka.getHost()
//		        + ":"
//		        + eureka.getMappedPort(8761)
//		        + "/eureka";
//
//		registry.add("eureka.client.enabled", () -> true);
//
//		registry.add("eureka.client.register-with-eureka", () -> false);
//
//		registry.add("eureka.client.fetch-registry", () -> false);
//
//		registry.add("eureka.client.service-url.defaultZone", () -> eurekaUrl);
//
//	}
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
//	@Autowired
//	EurekaClient eurekaClient;
//	
//	@Test
//	void shouldFindAuthServiceInEureka() throws Exception {
//
//	    // =========================================================
//	    // GET AUTH-SERVICE DA EUREKA
//	    // =========================================================
//
//	    String url =
//	            "http://"
//	            + eureka.getHost()
//	            + ":"
//	            + eureka.getMappedPort(8761)
//	            + "/eureka/apps/AUTH-SERVICE";
//
//	    log.info("Chiamata Eureka: GET {}", url);
//
//	    HttpClient client = HttpClient.newHttpClient();
//
//	    HttpRequest request = HttpRequest.newBuilder()
//	            .uri(URI.create(url))
//	            .GET()
//	            .build();
//
//	    HttpResponse<String> response =
//	            client.send(
//	                    request,
//	                    HttpResponse.BodyHandlers.ofString()
//	            );
//
//	    log.info("Eureka status: {}", response.statusCode());
//	    log.info("Eureka response:\n{}", response.body());
//
//	    // =========================================================
//	    // ASSERT
//	    // =========================================================
//
//	    assertThat(200).isEqualTo(response.statusCode());
//
//	    assertThat(
//	            response.body()
//	    ).contains("AUTH-SERVICE");
//
//	    assertThat(
//	            response.body()
//	    ).contains("auth-service-test");
//
//	    assertThat(
//	            response.body()
//	    ).contains(String.valueOf(authServiceMock.port()));
//	}
//
//	// =========================================================
//	// SIGNUP - NO TOKEN
//	// =========================================================
//
//	@Test
//	void givenNoToken_whenAccessingSignup_thenReturnsUnauthorized() {
//
//		webTestClient.post().uri("/api/users/signup").header("Content-Type", "application/json").bodyValue("""
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
//		webTestClient.post().uri("/api/users/signup").header("Authorization", "Bearer " + teacherToken)
//				.header("Content-Type", "application/json").bodyValue("""
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
//		InstanceInfo instance =
//	            eurekaClient.getNextServerFromEureka(
//	                    "AUTH-SERVICE",
//	                    false
//	            );
//
//	    log.info("Eureka instance: {}:{}", 
//	            instance.getHostName(),
//	            instance.getPort());
//
//		log.info("WireMock stub configurato per POST /api/users/signup");
//
//		webTestClient.post().uri("/api/users/signup").header("Authorization", "Bearer " + adminToken)
//				.header("Content-Type", "application/json").bodyValue("""
//						{
//						    "user": {
//						        "name": "John",
//						        "surname": "Doe",
//						        "email": "john@email.com",
//						        "password": "password",
//						        "roles": ["USER"]
//						    }
//						}
//						""").exchange().expectStatus().isOk();
//
//		log.info("Gateway ha restituito HTTP 200");
//
//		authServiceMock.verify(postRequestedFor(urlEqualTo("/api/users/signup")));
//
//		log.info("AUTH-SERVICE ha ricevuto POST /api/users/signup");
//	}
//
//	// =========================================================
//	// HELPER
//	// =========================================================
//
//	private String generateTokenWithRole(String email, String role) {
//
//		Map<String, Object> claims = new HashMap<>();
//
//		claims.put("authorities", Arrays.asList(role));
//
//		return jwtService.generateToken(claims, email);
//	}
//}