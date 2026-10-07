package com.rebel.teach;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.rebel.api.core.auth.JwtService;
import com.rebel.api.core.teach.LessonDateTimeRequest;
import com.rebel.api.core.teach.LessonDto;
import com.rebel.api.core.teach.TeacherController;
import com.rebel.api.exception.NotFoundException;
import com.rebel.teach.controller.TeacherControllerImpl;
import com.rebel.teach.entity.LessonEntity;
import com.rebel.teach.entity.TeacherEntity;
import com.rebel.teach.mapper.TeachServiceMapper;
import com.rebel.teach.repository.LessonRepository;
import com.rebel.teach.repository.TeacherRepository;
import com.rebel.util.UrlBuilder;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@ActiveProfiles("local")
@AutoConfigureWebTestClient(timeout = "10m")
@Transactional(propagation = Propagation.NOT_SUPPORTED) // NO ROLL BACK
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import(TestChannelBinderConfiguration.class)
class TeachServiceApplicationTests {
	
	@Value("${lessons}")
	Set<String> lessons;
	
	@Autowired
	JwtService jwtService;
	
	String adminToken;
	String teacherToken;
	String invalidTeacherToken;
	String staffToken;
	
	@Autowired
	TeachServiceMapper teachMapper;

	@Autowired
	ObjectMapper jsonMapper;
	
	@Autowired
	TeacherRepository teacherRepository;
	
	@Autowired
	LessonRepository lessonRepository;
	
	@Autowired
	WebTestClient client;
	
	/*
	 * URLS
	 */
	String myLessonsUrl;
	String registerLessonUrl;
	String helloUrl;
	String lessonTypesUrl;
	
	@BeforeAll
    void setupUrls() throws Exception {

		UrlBuilder urlBuilder = UrlBuilder.settingBaseUrl(
                TeacherControllerImpl.class
        );

		helloUrl = urlBuilder.buildGetUrl(
				TeacherController.class,
				"hello",
				new Class<?>[0]
		);
		
        registerLessonUrl = urlBuilder.buildPostUrl(
                TeacherController.class,
                "submitLessonDateTime",
                Jwt.class, LessonDateTimeRequest.class 
        );
        
        myLessonsUrl = urlBuilder.buildGetUrl(
				TeacherController.class,
				"myLessons",
				Jwt.class, Pageable.class
		);
        
        lessonTypesUrl = urlBuilder.buildGetUrl(
        		TeacherController.class,
        		"getLessonTypes",
        		new Class<?>[0]);
        
    }
	
	@BeforeAll
	void setUpDatabase() {

	    TeacherEntity mario = TeacherEntity.builder()
	    		.name("Mario")
	    		.surname("Rossi")
	    		.email("mario.rossi@gmail.com")
	    		.userId(98)
	    		.build();
	    
	    TeacherEntity luca = TeacherEntity.builder()
	    		.name("Luca")
	    		.surname("Bianchi")
	    		.email("luca.bianchi@gmail.com")
	    		.userId(99)
	            .build();
	    
	    teacherRepository.saveAll(List.of(mario, luca));
	    
	    LessonEntity lesson1 = LessonEntity.builder()
	            .name("GYM")
	            .date(LocalDate.of(2026, 8, 25))
	            .startTime(LocalTime.of(10, 30))
	            .endTime(LocalTime.of(11, 30))
	            .teacher(mario)
	            .build();
	    
	    LessonEntity lesson2 = LessonEntity.builder()
	            .name("DANCE")
	            .date(LocalDate.of(2026, 8, 26))
	            .startTime(LocalTime.of(12, 0))
	            .endTime(LocalTime.of(13, 0))
	            .teacher(mario)
	            .build();
	    
	    LessonEntity lesson3 = LessonEntity.builder()
	            .name("GYM")
	            .date(LocalDate.of(2026, 8, 27))
	            .startTime(LocalTime.of(14, 0))
	            .endTime(LocalTime.of(15, 0))
	            .teacher(luca)
	            .build();
	    
	    lessonRepository.saveAll(List.of(lesson1, lesson2, lesson3));
	    
	    /*
	     * TOKEN
	     */
	    Map<String, Object> claims = new HashMap<>();
	    claims.put("email", "admin.admin@rebel.com");
	    claims.put("roles", "ADMIN");
	    adminToken = jwtService.generateToken(claims);

	    claims.put("email", mario.getEmail());
	    claims.put("roles", "TEACHER");
	    teacherToken = jwtService.generateToken(claims);
	    invalidTeacherToken = teacherToken + "invalid";
	    
	    claims.put("email", "luca.bianchi@gmail.com");
	    claims.put("roles", "STAFF");
	    staffToken = jwtService.generateToken(claims);
	    
	}
	
	@Test
	void hello_returnsOkWithUnauthenticatedUser() throws Exception {

	    client
	    .get()
	    .uri(helloUrl)
	    .exchange()
	    .expectStatus()
	    .isOk();
	    
	}
	
	@Test
	void getLessons() throws Exception {
		
		String bodyResponse = client.get().uri(lessonTypesUrl)
		.accept(APPLICATION_JSON)
		.exchange()
		.expectStatus()
		.isOk()
		.expectBody(String.class)
		.returnResult().getResponseBody();
		
		Set<String> lessonsFromRequest = jsonMapper.readValue(bodyResponse, new TypeReference<Set<String>>() {});
		
		assertThat(lessonsFromRequest).isNotEmpty();
		assertThat(lessonsFromRequest).isEqualTo(lessons);
		
	}
	
	@Test
	void getLessons_returnsPagedLessons_whenTeacher() throws Exception {
		
		 // Chiamata API
	    String bodyResponse = client.get()
	    		.uri(myLessonsUrl)
	    		.accept(APPLICATION_JSON)
	    		.header("Authorization", "bearer " + teacherToken)
	    		.exchange()
	    		.expectStatus()
	    		.isOk()
	    		.expectBody(String.class)
	    		.returnResult()
	    		.getResponseBody();
	    
	    
	    List<String> lessonsFromResponse = jsonMapper
	    		.readTree(bodyResponse)
	    		.get("content")
	    		.findValuesAsString("name");
	    		
	 // Recupera le lezioni dal DB
	    List<LessonDto> expectedLessons = lessonRepository
	            .findByTeacherEmail(
	                    jwtService.extractEmail(teacherToken),
	                    Pageable.unpaged()
	            )
	            .getContent()
	            .stream()
	            .map(teachMapper::toDto)
	            .toList();
	    
	    assertThat(expectedLessons).extracting(LessonDto::name)
	    .containsExactlyInAnyOrderElementsOf(lessonsFromResponse);
	    
	}

	@Test
	void registerLesson_returnsCreated() throws Exception {

		String lessonName = "GYM";
	    LocalDate date = LocalDate.of(2026, 8, 25);
	    LocalTime startTime = LocalTime.of(10, 30);
	    LocalTime endTime = LocalTime.of(11, 30);

	    LessonDateTimeRequest bodyRequest = new LessonDateTimeRequest(
	            lessonName,
	            date,
	            startTime,
	            endTime
	    );

	    Integer lessonId = client.post()
	    		.uri(registerLessonUrl)
	    		.header("Authorization", "Bearer " + teacherToken)
	    		.contentType(APPLICATION_JSON)
	    		.bodyValue(bodyRequest)
	    		.exchange()
	    		.expectStatus().isCreated()
	    		.expectBody(Integer.class)
	    		.returnResult()
	    		.getResponseBody();
	    		
	    assertThat(lessonId).isNotNull();		
	    
	    // Recupera le lezioni dal DB
	    LessonEntity lesson = lessonRepository.findById(lessonId)
	    		.orElseThrow(() -> new NotFoundException("Lesson not found with id: " + lessonId));

	    // Controlla i dati della lezione
	    assertThat(lesson.getName()).isEqualTo(lessonName);
	    assertThat(lesson.getDate()).isEqualTo(date);
	    assertThat(lesson.getStartTime()).isEqualTo(startTime);
	    assertThat(lesson.getEndTime()).isEqualTo(endTime);

	    // Controlla il proprietario della lezione
	    assertThat(lesson.getTeacher().getEmail())
	            .isEqualTo(jwtService.extractEmail(teacherToken));
	    
	}
	
	@Test
	void registerLesson_returnsBadRequestWhenLessonTypeDoesNotExist() throws Exception {

		String lessonName = "SWIMMING";
	    LocalDate date = LocalDate.of(2026, 8, 25);
	    LocalTime startTime = LocalTime.of(10, 30);
	    LocalTime endTime = LocalTime.of(11, 30);
	    
	    LessonDateTimeRequest bodyRequest = new LessonDateTimeRequest(
	            lessonName,
	            date,
	            startTime,
	            endTime
	    );

	    client.post()
	    .uri(registerLessonUrl)
	    .contentType(APPLICATION_JSON)
	    .header("Authorization", "Bearer " + teacherToken)
	    .bodyValue(bodyRequest)
	    .exchange()
	    .expectStatus().isBadRequest()
	    .expectBody()
	    .jsonPath("$.message")
	    .isEqualTo("Invalid lesson type: SWIMMING");
	   
	}
	
}