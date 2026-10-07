package com.rebel.auth;

import static com.rebel.api.core.event.Type.CREATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.rebel.api.core.auth.AuthController;
import com.rebel.api.core.auth.JwtService;
import com.rebel.api.core.auth.LoginResponse;
import com.rebel.api.core.auth.LoginUser;
import com.rebel.api.core.auth.User;
import com.rebel.api.core.event.Event;
import com.rebel.auth.controller.AuthControllerImpl;
import com.rebel.auth.entity.UserEntity;
import com.rebel.auth.repo.UserRepository;
import com.rebel.auth.service.SequenceGeneratorService;
import com.rebel.util.UrlBuilder;

import reactor.test.StepVerifier;
import tools.jackson.databind.ObjectMapper;

@ActiveProfiles("local")
@AutoConfigureWebTestClient
@Testcontainers
@Transactional(propagation = Propagation.NOT_SUPPORTED) // NO ROLL BACK
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import(TestChannelBinderConfiguration.class)
@TestMethodOrder(OrderAnnotation.class)
class AuthServiceApplicationTests extends MyNoSqlTestBase {

	@Autowired
	ObjectMapper objectMapper;
	
	@Autowired
	WebTestClient client;
	
	@Autowired
	JwtService jwtService;
	
	@MockitoBean
	StreamBridge streamBridge;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    UserRepository userRepository;
    
    @Autowired
    SequenceGeneratorService sequenceGenerator;

    String adminToken;
    String teacherToken;
    String invalidToken;
    
    String signupUrl;
    String loginUrl;
    String helloUrl;
    String usersUrl;
    
    // =========================================================
    // BUILD URLS
    // =========================================================
    
    @BeforeAll
    void setupUrls() throws Exception {

        UrlBuilder urlBuilder =
                UrlBuilder.settingBaseUrl(AuthControllerImpl.class);

        signupUrl = urlBuilder.buildPostUrl(
                AuthController.class,
                "register",
                User.class
        );

        loginUrl = urlBuilder.buildPostUrl(
        		AuthController.class,
                "authenticate",
                LoginUser.class
        );
        
        helloUrl = urlBuilder.buildGetUrl(
        		AuthController.class,
                "hello"
        );
        
        usersUrl = urlBuilder.buildGetUrl(
                AuthController.class,
                "getUsers",
                Pageable.class
        );
        
    }
    
    @Test
    void getHello_withUnauthenticatedUser() {

        client
            .get()
            .uri(helloUrl)
            .exchange()
            .expectStatus().isOk();
    }
    
    @Test
    void givenNonExistentUser_whenLogin_thenReturnsNotFoundException() {

        // Given
        LoginUser loginUser = new LoginUser(
            "pippo.franco@rebel.com",
            "password"
        );

        // When
        client
        .post()
        .uri(loginUrl)
        .bodyValue(loginUser)
        .exchange()
        .expectStatus().isNotFound()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo(
                "User NOT found with email: " + loginUser.email()
        );
        
    }
    
    // =========================================================
    // GENERATE USERS
    // =========================================================
    
    @Test
    @Order(1)
    void setupUsers() throws Exception {
    	
        UserEntity admin = UserEntity.builder()
        		.id(sequenceGenerator.generateSequence(UserEntity.SEQUENCE_NAME)
        		        .block())
        		.name("admin")
        		.surname("admin")
                .email("admin@rebel.com")
                .password(passwordEncoder.encode("password"))
                .role("ADMIN")
                .build();
        
        admin = userRepository.save(admin).block();

        UserEntity teacher = UserEntity.builder()
        		.id(sequenceGenerator.generateSequence(UserEntity.SEQUENCE_NAME)
        		        .block())
        		.name("teacher")
        		.surname("teacher")
                .email("teacher@rebel.com")
                .password(passwordEncoder.encode("password"))
                .role("TEACHER")
                .build();
        
        teacher = userRepository.save(teacher).block();
        
        StepVerifier
        .create(userRepository.findAll().collectList())
        .assertNext(users -> assertThat(users).isNotEmpty())
        .verifyComplete();
        
    }
    
    @Test
    @Order(2)
    void givenInvalidPassword_whenLogin_thenReturnsBadRequest() {
    	
    	LoginUser adminLoginUser = new LoginUser(
        		"admin@rebel.com",
        		"password123"
        );
    	
    	client
        .post()
        .uri(loginUrl)
        .contentType(APPLICATION_JSON)
        .bodyValue(adminLoginUser)
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody()
        .consumeWith(response -> {
            System.out.println(new String(response.getResponseBody()));
        })
        .jsonPath("$.message")
        .isEqualTo("Invalid Password");
    	
    }
    
    // =========================================================
    // GENERATE TOKENS
    // =========================================================
    
    @Test
    @Order(3)
    void generateTokens() {
    	
    	LoginUser adminLoginUser = new LoginUser(
        		"admin@rebel.com",
        		"password"
        );

	LoginResponse adminResponse = client
		    .post()
		    .uri(loginUrl)
		    .contentType(APPLICATION_JSON)
		    .bodyValue(adminLoginUser)
		    .exchange()
		    .expectStatus().isOk()
		    .expectBody(LoginResponse.class)
		    .returnResult()
		    .getResponseBody();

		adminToken = adminResponse.token();
        assertThat(adminToken).isNotBlank();
        
        LoginUser teacherLoginUser = new LoginUser(
        		"teacher@rebel.com",
        		"password"
        );
        
        LoginResponse teacherResponse = client
        	    .post()
        	    .uri(loginUrl)
        	    .contentType(APPLICATION_JSON)
        	    .bodyValue(teacherLoginUser)
        	    .exchange()
        	    .expectStatus().isOk()
        	    .expectBody(LoginResponse.class)
        	    .returnResult()
        	    .getResponseBody();

    	teacherToken = teacherResponse.token();
        assertThat(teacherToken).isNotBlank();
    	
    }
    
    // =========================================================
    // SIGNUP TESTS
    // =========================================================
    
    @Test
    @Order(4)
    void givenValidRequest_whenCreatingAdmin_thenAdminIsCreated() {
    	
    	String email = "giuseppe.verdi@rebel.com";
    	String name = "Giuseppe";
    	String surname = "Verdi";
    	String role = "ADMIN";
    	
    	// given
    	String requestBody = """
    			{
    			    "name": "Giuseppe",
    			    "surname": "Verdi",
    			    "email": "giuseppe.verdi@rebel.com",
    			    "password": "password",
    			    "role": "ADMIN"
    			}
    			""";

    	// when
    	client
    	.post().uri(signupUrl)
    	.contentType(APPLICATION_JSON)
    	.bodyValue(requestBody)
    	.header("Authorization", "bearer " + adminToken)
    	.exchange()
    	.expectStatus().isCreated();
    	
        StepVerifier.create(userRepository.findByEmail(email))
        .assertNext(user -> {
        	assertThat(user.getId()).isPositive();
        	assertThat(user.getEmail()).isEqualTo(email);
        	assertThat(user.getName()).isEqualTo(name);
        	assertThat(user.getSurname()).isEqualTo(surname);
        	assertThat(passwordEncoder.matches(
        		    "password",
        		    user.getPassword()
        		)).isTrue();
        	assertThat(user.getRole()).isEqualTo(role);
        }).verifyComplete();

    }
    
    @Test
    @Order(5)
    void givenValidRequest_whenCreatingTeacher_thenTeacherIsCreated() {

        // given
//    	doReturn(true)
//    	.when(streamBridge)
//    	.send( eq("teacher-created-event-out-0"), any() );
    	
    	when(streamBridge.send(eq("teacher-created-event-out-0"), any())).thenReturn(true);

        String email = "paola.neri@rebel.com";
        String name = "Paola";
        String surname = "Neri";
        String role = "TEACHER";

        String requestBody = """
                {
                    "name": "Paola",
                    "surname": "Neri",
                    "email": "paola.neri@rebel.com",
                    "password": "password",
                    "role": "TEACHER"
                }
                """;

        // when
        Integer teacherId = client
            .post()
            .uri(signupUrl)
            .contentType(APPLICATION_JSON)
            .bodyValue(requestBody)
            .header("Authorization", "bearer " + adminToken)
            .exchange()
            .expectStatus().isAccepted()
            .expectBody(new ParameterizedTypeReference<Integer>() {})
            .returnResult()
            .getResponseBody();

        // then
        verify(streamBridge).send(
            eq("teacher-created-event-out-0"),
            argThat(message -> {
            	
            	GenericMessage<Event<Integer, User>> genericMessage = (GenericMessage<Event<Integer, User>>) message;
                
                Event<Integer, User> event = genericMessage.getPayload();
                User teacher = genericMessage.getPayload().data();

                return event.type() == CREATE
                    && event.key().equals(teacherId)
                    && teacher.id().equals(teacherId)
                    && teacher.name().equals(name)
                    && teacher.surname().equals(surname)
                    && teacher.email().equals(email)
                    && teacher.role().equals(role);
            })
        );
    }
    
    @Test
    @Order(6)
    void givenUsers_whenGetUsers_thenReturnsUsers() {

        client
            .get()
            .uri(uriBuilder -> uriBuilder
                    .path(usersUrl)
                    .queryParam("page", 0)
                    .queryParam("size", 20)
                    .build())
            .header("Authorization", "Bearer " + adminToken)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.content").isArray()
            .jsonPath("$.content").isNotEmpty()
            .jsonPath("$.totalElements").isNumber()
            .jsonPath("$.totalElements").value(value -> assertThat((Integer) value).isPositive())
            .jsonPath("$.number").isEqualTo(0)
            .jsonPath("$.size").isEqualTo(20)
            ;
        
    }


}
