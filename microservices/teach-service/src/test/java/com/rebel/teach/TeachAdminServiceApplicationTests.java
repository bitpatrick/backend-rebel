package com.rebel.teach;
import static com.rebel.teach.integration.Integration.CIRCUIT_BREAKER_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.rebel.api.core.auth.JwtService;
import com.rebel.api.core.auth.User;
import com.rebel.api.core.teach.CreateTeacherRequest;
import com.rebel.api.core.teach.TeachAdminController;
import com.rebel.teach.controller.TeachAdminControllerImpl;
import com.rebel.teach.entity.TeacherEntity;
import com.rebel.teach.mapper.TeachMapper;
import com.rebel.teach.repository.LessonRepository;
import com.rebel.teach.repository.TeacherRepository;
import com.rebel.util.ServiceUtil;
import com.rebel.util.UrlBuilder;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@TestMethodOrder(OrderAnnotation.class)
@ActiveProfiles("local")
@AutoConfigureWebTestClient(timeout = "10m")
@Transactional(propagation = Propagation.NOT_SUPPORTED) // NO ROLL BACK
// Il test usa H2 in memoria e non richiede la console HTTP sulla porta 8082.
@SpringBootTest(properties = {
    "h2-server.enabled=false",
    "management.endpoints.web.exposure.include=health,metrics",
    "management.endpoint.health.show-details=always",
    "management.health.circuitbreakers.enabled=true",
    // Registra il breaker all'avvio per poter leggere la metrica prima della POST.
    "resilience4j.circuitbreaker.instances.create-teacher-sync.register-health-indicator=true"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import({
    TestChannelBinderConfiguration.class,
    MyConfiguration.class
})
class TeachAdminServiceApplicationTests {
	
	private static final String CIRCUIT_BREAKER_HEALTH_ENDPOINT = "/actuator/health/circuitBreakers";

	@Autowired
	ServiceUtil serviceUtil;
	
	@Value("${lessons}")
	Set<String> lessons;
	
	@Autowired
	JwtService jwtService;
	
	String adminJWT;
	String teacherJWT;
	String invalidJWT;
	String staffJWT;
	
	@Autowired
	TeachMapper teachMapper;

	@Autowired
	ObjectMapper objectMapper;
	
	@Autowired
	TeacherRepository teacherRepository;
	
	@Autowired
	LessonRepository lessonRepository;
	
	@Autowired
	WebTestClient client;
	
	/*
	 * URLS
	 */
	String helloUrl;
	String teachersUrl;
	String registerLessonUrl;
	String createTeacherUrl;
	
	private static MockWebServer mockAuthServer = createMockServer();

	private static MockWebServer createMockServer() {
	    try {
	        MockWebServer server = new MockWebServer();
	        server.start();
	        System.out.println(String.format(
	                "AuthMockWebServer avviato - URI: %s, Port: %d",
	                server.url("/"),
	                server.getPort()
	        ));
	        return server;
	    } catch (IOException e) {
	        throw new RuntimeException(e);
	    }
	}

	@AfterAll
	static void stopMockServer() throws IOException {
	    mockAuthServer.shutdown();
	}
	
	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		String createUserEndpoint = String.format("http://fakehost:%s", mockAuthServer.getPort());
		System.out.println("createUserEndpoint: " + createUserEndpoint);
	    registry.add(
	        "app.auth-service.url",
	        () -> createUserEndpoint
	    );
	}
	
	@BeforeAll
    void setupUrls() throws Exception {

		UrlBuilder urlBuilder = UrlBuilder.settingBaseUrl(
				TeachAdminControllerImpl.class
        );
		
		helloUrl = urlBuilder.buildGetUrl(
        		TeachAdminController.class,
				"hello",
				new Class<?>[0]
		);

        teachersUrl = urlBuilder.buildGetUrl(
                TeachAdminController.class,
                "teachers",
                String.class,
                String.class,
                Pageable.class
        );
        
        createTeacherUrl = urlBuilder.buildPostUrl(
        		TeachAdminController.class, 
        		"createTeacherSync",
				int.class,
				int.class,
        		Jwt.class,
        		CreateTeacherRequest.class);

    }
	
	@BeforeAll
	void generateTokens() {

	    Map<String, Object> claims = new HashMap<>();
	    claims.put("email", "mario.rossi@rebel.com");
	    claims.put("roles", "ADMIN");
	    adminJWT = jwtService.generateToken(claims);

	    claims.put("email", "giulia.romano@rebel.com");
	    claims.put("roles", "TEACHER");
	    teacherJWT = jwtService.generateToken(claims);
	    
	}
	
	@Test
	@Order(1)
	void hello_returnsOkWithUnauthenticatedUser() throws Exception {

	    client.get()
	    .uri(helloUrl)
	    .accept(APPLICATION_JSON)
	    .exchange()
	    .expectStatus()
	    .isOk();
	    
	}
	
	@Test
	@Order(2)
	void getTeachers_returnsPagedTeachers_whenAdmin() throws Exception {
		
		client.get()
		.uri(teachersUrl)
		.accept(APPLICATION_JSON)
		.header("Authorization", "Bearer " + adminJWT)
		.exchange()
		.expectStatus()
		.isOk();
		
	}
	
	@Test
	@Order(3)
	void getTeacher_returnsTeacher_whenAdmin() {

	    // ID dell'insegnante creato nel setup
	    Integer teacherId = 919;

	    client.get()
	        .uri(teachersUrl + "/{id}", teacherId)
	        .accept(APPLICATION_JSON)
	        .header("Authorization", "Bearer " + adminJWT)
	        .exchange()
	        .expectStatus().isOk()
	        .expectBody()
	        .jsonPath("$.id").isEqualTo(teacherId)
	        .jsonPath("$.name").isEqualTo("Marco")
	        .jsonPath("$.surname").isEqualTo("Santoro")
	        .jsonPath("$.email").isEqualTo("marco.santoro@rebel.com");
	}

	@Test
	@Order(4)
	void getTeacher_returnsNotFoundWhenTeacherDoesNotExist_whenAdmin() {

	    // ID che non esiste nel database
	    Integer teacherId = 9999;

	    client.get()
	        .uri(teachersUrl + "/{id}", teacherId)
	        .accept(APPLICATION_JSON)
	        .header("Authorization", "Bearer " + adminJWT)
	        .exchange()
	        .expectStatus().isNotFound();
	}
	
	@Test
	@Order(5)
	void getTeacherLessons_returnsBadRequestWhenTeacherIdIsNegative_whenAdmin() {

	    Integer teacherId = -1;

	    client.get()
	        .uri(teachersUrl + "/{id}/lessons", teacherId)
	        .accept(APPLICATION_JSON)
	        .header("Authorization", "Bearer " + adminJWT)
	        .exchange()
	        .expectStatus().isBadRequest();
	}

	@Test
	@Order(6)
	void getParamsTeachers_returnsOk_whenAdmin() {

	    client.get()
	        .uri(uriBuilder -> uriBuilder
	            .path(teachersUrl)
	            .queryParam("name", "a")
	            .queryParam("surname", "f")
	            .build())
	        .accept(APPLICATION_JSON)
	        .header("Authorization", "Bearer " + adminJWT)
	        .exchange()
	        .expectStatus().isOk()
	        .expectBody()
	        .jsonPath("$.content.length()").isEqualTo(2)
	        .jsonPath("$.content[0].id").isEqualTo(906)
	        .jsonPath("$.content[1].id").isEqualTo(917)
	        .jsonPath("$.numberOfElements").isEqualTo(2)
	        .jsonPath("$.totalElements").isEqualTo(2)
	        .jsonPath("$.totalPages").isEqualTo(1)
	        .jsonPath("$.number").isEqualTo(0)
	        .jsonPath("$.size").isEqualTo(20)
	        .jsonPath("$.first").isEqualTo(true)
	        .jsonPath("$.last").isEqualTo(true);
	}
	
	/*
	 * START TEST PAGINATION
	 */
	
	@Test
	@Order(7)
	void getTeachers_returnsCorrectPages() {

	    Pageable nextPage = PageRequest.of(
	        0, 2, Sort.by("id").ascending()
	    );

	    nextPage = testNextPage(nextPage, "[905, 906]", true);
	    nextPage = testNextPage(nextPage, "[907, 908]", true);
	    nextPage = testNextPage(nextPage, "[909, 910]", true);
	    nextPage = testNextPage(nextPage, "[911, 912]", true);
	    nextPage = testNextPage(nextPage, "[913, 914]", true);
	    nextPage = testNextPage(nextPage, "[915, 916]", true);
	    nextPage = testNextPage(nextPage, "[917, 918]", true);

	    testNextPage(nextPage, "[919]", false);
	}	
	
	@Order(8)
	@Test
	void getTeacherLessons_returnsLessons_whenAdmin() {

	    Integer teacherId = 906; // Andrea Ferrari

	    client.get()
	        .uri(teachersUrl + "/{id}/lessons", teacherId)
	        .accept(APPLICATION_JSON)
	        .header("Authorization", "Bearer " + adminJWT)
	        .exchange()
	        .expectStatus().isOk()
	        .expectBody()
	        .jsonPath("$.content.length()").isEqualTo(2)
	        .jsonPath("$.totalElements").isEqualTo(2)
	        .jsonPath("$.content[0].name").isEqualTo("GYM")
	        .jsonPath("$.content[1].name").isEqualTo("DANCE");
	}
	
	private Pageable testNextPage(
	        Pageable nextPage,
	        String expectedTeacherIds,
	        boolean expectsNextPage) {

	    // Recupera la pagina richiesta dal database
	    Page<TeacherEntity> teacherPage = teacherRepository.findAll(nextPage);

	    // Verifica gli ID dei teacher
	    assertEquals(
	        expectedTeacherIds,
	        teacherPage.getContent()
	            .stream()
	            .map(TeacherEntity::getId)
	            .collect(Collectors.toList())
	            .toString()
	    );

	    // Verifica se esiste una pagina successiva
	    assertEquals(expectsNextPage, teacherPage.hasNext());

	    return teacherPage.nextPageable();
	}
	
	/*
	 * END TEST PAGINATION
	 */
	
	@Order(9)
	@Test
	void createSyncTeacher() throws InterruptedException {

	    // 1. Preparo i dati della richiesta di registrazione
	    String name = "Pinco";
	    String surname = "Pallino";
	    String email = "pinco.pallino@rebel.com";
	    String password = "password";
	    String serviceAddress = serviceUtil.getServiceAddress();

	    CreateTeacherRequest request = new CreateTeacherRequest(
	            name, surname, email, password, serviceAddress
	    );

	    // 2. Preparo la risposta simulata di Auth Service
	    // Auth Service restituisce un User dopo aver registrato l'utente
	    int userId = 999;
	    User user = new User(userId, name, surname, email, password, "TEACHER", serviceAddress);
	    

	    mockAuthServer.enqueue(
	            new MockResponse()
	                    .setResponseCode(CREATED.value())
	                    .addHeader("Content-Type", APPLICATION_JSON)
	                    .setBody(objectMapper.writeValueAsString(user))
	    );

	    // 3. Invoco l'endpoint di registrazione di Teach Service
	    // Teach Service chiamerà Auth Service tramite WebClient
	    client.post()
	            .uri(createTeacherUrl)
	            .header("Authorization", "Bearer " + adminJWT)
	            .contentType(APPLICATION_JSON)
	            .accept(APPLICATION_JSON)
	            .bodyValue(request)
	            .exchange()

	            // 4. Verifico la risposta HTTP di Teach Service
	            .expectStatus().isCreated()

	            // 5. Verifico il Teacher restituito nel response body
	            .expectBody()
	            .jsonPath("$.id").isNumber()
	            .jsonPath("$.name").isEqualTo(name)
	            .jsonPath("$.surname").isEqualTo(surname)
	            .jsonPath("$.email").isEqualTo(email)
	            .jsonPath("$.userId").isEqualTo(userId);

	    // 6. Verifico la richiesta HTTP ricevuta da Mock Auth Service
	    RecordedRequest recordedRequest = mockAuthServer.takeRequest(5, TimeUnit.SECONDS);
	    assertThat(recordedRequest).isNotNull();

	    assertEquals("POST", recordedRequest.getMethod());
	    assertEquals("/api/sync-signup?delay=0&faultPercent=0", recordedRequest.getPath());
	    assertEquals(
	            "Bearer " + adminJWT,
	            recordedRequest.getHeader("Authorization")
	    );
	}
	
	/*
	 * TEST CIRCUIT BREAKER
	 */

	@Order(10)
    @Test
    void createSyncTeacherReturnsUnavailableOnFallbackWithoutSaving() throws InterruptedException {
    	
        long teachersBefore = teacherRepository.count();
        JsonNode healthBefore = createTeacherCircuitBreakerHealthDetails()
        	    .required("details")
        	    .required(CIRCUIT_BREAKER_NAME)
        	    .required("details");
        
        String name = "Pizza";
	    String surname = "Balla";
	    String email = "pizza.balla@rebel.com";
	    String password = "password";
	    String serviceAddress = serviceUtil.getServiceAddress();
        CreateTeacherRequest request = new CreateTeacherRequest(name, surname, email, password, serviceAddress);

        // Un errore HTTP di Auth attiva il fallback reale dell'integrazione.
        mockAuthServer.enqueue(new MockResponse().setResponseCode(500));

        // chiamo l endpoint e mi aspetto che torni 503
        client.post()
                .uri(createTeacherUrl)
                .header("Authorization", "Bearer " + adminJWT)
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.id").isEmpty()
                .jsonPath("$.userId").isEmpty()
                .jsonPath("$.name").isEqualTo("Fallback user")
                .jsonPath("$.surname").isEqualTo("Fallback user")
                .jsonPath("$.email").isEqualTo("Fallback user");
        
        JsonNode healthAfter = createTeacherCircuitBreakerHealthDetails().required("details").required(CIRCUIT_BREAKER_NAME).required("details");
        assertThat(healthAfter.required("bufferedCalls").intValue())
                .as("bufferedCalls in circuit breaker health")
                .isEqualTo(healthBefore.required("bufferedCalls").intValue() + 1);
        assertThat(healthAfter.required("failedCalls").intValue())
                .as("failedCalls in circuit breaker health")
                .isEqualTo(healthBefore.required("failedCalls").intValue() + 1);

        assertThat(teacherRepository.count()).isEqualTo(teachersBefore);
        assertThat(teacherRepository.existsByEmail(email)).isFalse();
        assertThat(mockAuthServer.takeRequest(5, TimeUnit.SECONDS)).isNotNull();
    }
    
	@Order(11)
    @Test
    void createSyncTeacherReturnsCloseStateAfter3Requests() {
    	
        String name = "Attila";
	    String surname = "Unno";
	    String email = "attila.unno@rebel.com";
	    String password = "password";
	    String serviceAddress = serviceUtil.getServiceAddress();
        CreateTeacherRequest request = new CreateTeacherRequest(name, surname, email, password, serviceAddress);
        IntStream.iterate(1, i -> i <= 3, i -> {
        	
        	// chiamo l endpoint e mi aspetto che torni 503
            client.post()
                    .uri(createTeacherUrl)
                    .header("Authorization", "Bearer " + adminJWT)
                    .contentType(APPLICATION_JSON)
                    .accept(APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isEqualTo(503)
                    .expectBody()
                    .jsonPath("$.id").isEmpty()
                    .jsonPath("$.userId").isEmpty()
                    .jsonPath("$.name").isEqualTo("Fallback user")
                    .jsonPath("$.surname").isEqualTo("Fallback user")
                    .jsonPath("$.email").isEqualTo("Fallback user");
        	
        	return i;
        });
        
        JsonNode healtAfter = createTeacherCircuitBreakerHealthDetails();
        assertThat(healtAfter.required("status").stringValue()).as("Aggregate state of the Circuit Breakers").isEqualTo("UP");
        assertThat(healtAfter.required("details").required(CIRCUIT_BREAKER_NAME).required("details").required("state").stringValue()).as("State of the circuit breaker").isEqualTo("CLOSED");
        assertThat(healtAfter.required("details").required(CIRCUIT_BREAKER_NAME).required("details").required("state").stringValue()).as("State of the circuit breaker").isEqualTo("CLOSED");

    }

	@Order(12)
    @Test
    void createSyncTeacherReturnsNotFoundWithoutCountingFailure_whenAuthReturns404() throws InterruptedException {
		assertAuthDomainErrorIsPropagatedAndIgnored(404, "Not Found", "Role not found", "mario.rossi@rebel.com");
    }

	@Order(13)
    @Test
    void createSyncTeacherReturnsBadRequestWithoutCountingFailure_whenAuthReturns400() throws InterruptedException {
		assertAuthDomainErrorIsPropagatedAndIgnored(400, "Bad Request", "User already exists", "luigi.verdi@rebel.com");
    }

	/**
	 * Auth Service risponde con un errore di dominio: Teach Service deve restituire lo stesso status
	 * con il messaggio originale, senza fallback, senza salvare e senza registrare la chiamata nel circuit breaker.
	 */
	private void assertAuthDomainErrorIsPropagatedAndIgnored(int status, String error, String message, String email)
			throws InterruptedException {

        long teachersBefore = teacherRepository.count();
        JsonNode healthBefore = createTeacherCircuitBreakerHealthDetails()
        	    .required("details").required(CIRCUIT_BREAKER_NAME).required("details");

        CreateTeacherRequest request = new CreateTeacherRequest(
        		"Nome", "Cognome", email, "password", serviceUtil.getServiceAddress());

        // Corpo HttpErrorInfo come lo produce il GlobalControllerExceptionHandler di Auth Service.
        mockAuthServer.enqueue(new MockResponse()
        		.setResponseCode(status)
        		.addHeader("Content-Type", APPLICATION_JSON)
        		.setBody("""
        				{"path":"/api/sync-signup","status":%d,"error":"%s","message":"%s"}
        				""".formatted(status, error, message)));

        client.post()
                .uri(createTeacherUrl)
                .header("Authorization", "Bearer " + adminJWT)
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isEqualTo(status)
                .expectBody()
                .jsonPath("$.message").isEqualTo(message);

        JsonNode healthAfter = createTeacherCircuitBreakerHealthDetails()
        		.required("details").required(CIRCUIT_BREAKER_NAME).required("details");
        assertThat(healthAfter.required("failedCalls").intValue())
                .as("failedCalls in circuit breaker health")
                .isEqualTo(healthBefore.required("failedCalls").intValue());
        assertThat(healthAfter.required("bufferedCalls").intValue())
                .as("bufferedCalls in circuit breaker health")
                .isEqualTo(healthBefore.required("bufferedCalls").intValue());

        assertThat(teacherRepository.count()).isEqualTo(teachersBefore);
        assertThat(teacherRepository.existsByEmail(email)).isFalse();
        assertThat(mockAuthServer.takeRequest(5, TimeUnit.SECONDS)).isNotNull();
	}

    private JsonNode createTeacherCircuitBreakerHealthDetails() {
        
    	JsonNode health = client.get()
                .uri(CIRCUIT_BREAKER_HEALTH_ENDPOINT)
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(JsonNode.class)
                .returnResult()
                .getResponseBody();

        assertThat(health).isNotNull();
        return health;
    }
    
}
