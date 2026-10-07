//package com.rebel.gateway;
//
//import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
//import static com.github.tomakehurst.wiremock.client.WireMock.get;
//import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
//import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
//
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.TestInstance;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.web.server.LocalServerPort;
//import org.springframework.http.MediaType;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.web.reactive.server.WebTestClient;
//
//import com.github.tomakehurst.wiremock.WireMockServer;
//import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
//
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@ActiveProfiles("local")
//class GatewayServiceApplicationTests {
//
//	@LocalServerPort
//	private int port;
//
//	private WebTestClient webTestClient;
//	private WireMockServer authServiceMock; // microservice
//	private WireMockServer teachServiceMock; // microservice
//
//	
//	void setUp() {
//		// Avvia i mock server per i servizi backend
//		authServiceMock = new WireMockServer(
//			WireMockConfiguration.wireMockConfig().port(8081)
//		);
//		authServiceMock.start();
//
//		teachServiceMock = new WireMockServer(
//			WireMockConfiguration.wireMockConfig().port(8082)
//		);
//		teachServiceMock.start();
//
//		// Crea il client per testare il gateway
//		webTestClient = WebTestClient.bindToServer()
//			.baseUrl("http://localhost:" + port)
//			.build();
//	}
//
//	@AfterEach
//	void tearDown() {
//		authServiceMock.stop();
//		teachServiceMock.stop();
//	}
//
//	/**
//	 * Test: Verifica che una richiesta a /api/auth/** venga inoltrata al auth-service
//	 */
//	@Test
//	void testAuthServiceRouting() {
//		// Configura il mock per auth-service
//		authServiceMock.stubFor(get(urlEqualTo("/api/auth/health"))
//			.willReturn(aResponse()
//				.withStatus(200)
//				.withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
//				.withBody("{\"status\":\"UP\"}")));
//
//		// Effettua la richiesta tramite gateway
//		webTestClient.get()
//			.uri("/api/auth/health")
//			.exchange()
//			.expectStatus().isOk()
//			.expectBody(String.class)
//			.isEqualTo("{\"status\":\"UP\"}");
//
//		// Verifica che il mock è stato chiamato
//		authServiceMock.verify(getRequestedFor(urlEqualTo("/api/auth/health")));
//	}
//
//	/**
//	 * Test: Verifica che una richiesta a /api/users/** venga inoltrata al auth-service
//	 */
//	@Test
//	void testUsersServiceRouting() {
//		// Configura il mock per auth-service (rotta /api/users)
//		authServiceMock.stubFor(get(urlEqualTo("/api/users/1"))
//			.willReturn(aResponse()
//				.withStatus(200)
//				.withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
//				.withBody("{\"id\":1,\"name\":\"John Doe\"}")));
//
//		// Effettua la richiesta tramite gateway
//		webTestClient.get()
//			.uri("/api/users/1")
//			.exchange()
//			.expectStatus().isOk()
//			.expectBody(String.class)
//			.isEqualTo("{\"id\":1,\"name\":\"John Doe\"}");
//
//		// Verifica che il mock è stato chiamato
//		authServiceMock.verify(getRequestedFor(urlEqualTo("/api/users/1")));
//	}
//
//	/**
//	 * Test: Verifica che una richiesta a /api/teachers/** venga inoltrata al teach-service
//	 */
//	@Test
//	void testTeachersServiceRouting() {
//		// Configura il mock per teach-service
//		teachServiceMock.stubFor(get(urlEqualTo("/api/teachers/1"))
//			.willReturn(aResponse()
//				.withStatus(200)
//				.withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
//				.withBody("{\"id\":1,\"name\":\"Prof. Smith\"}")));
//
//		// Effettua la richiesta tramite gateway
//		webTestClient.get()
//			.uri("/api/teachers/1")
//			.exchange()
//			.expectStatus().isOk()
//			.expectBody(String.class)
//			.isEqualTo("{\"id\":1,\"name\":\"Prof. Smith\"}");
//
//		// Verifica che il mock è stato chiamato
//		teachServiceMock.verify(getRequestedFor(urlEqualTo("/api/teachers/1")));
//	}
//
//}
