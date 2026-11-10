package com.rebel.teach;
import static java.util.stream.IntStream.rangeClosed;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
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
import static org.springframework.http.HttpStatus.*;
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
import com.rebel.teach.mapper.TeachMapper;
import com.rebel.teach.repository.LessonRepository;
import com.rebel.teach.repository.TeacherRepository;
import com.rebel.util.UrlBuilder;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
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
	TeachMapper teachMapper;

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
	    
	}
	
	@BeforeAll
	void generateTokens() {
		
		Map<String, Object> claims = new HashMap<>();
	    claims.put("email", "admin.admin@rebel.com");
	    claims.put("roles", "ADMIN");
	    adminToken = jwtService.generateToken(claims);

	    claims.put("email", "mario.rossi@gmail.com");
	    claims.put("roles", "TEACHER");
	    teacherToken = jwtService.generateToken(claims);
	    invalidTeacherToken = teacherToken + "invalid";
	    
	    claims.put("email", "luca.bianchi@gmail.com");
	    claims.put("roles", "STAFF");
	    staffToken = jwtService.generateToken(claims);
		
	}
	
	@Test
	@Order(1)
	void hello_returnsOkWithUnauthenticatedUser() {

	    client
	    .get()
	    .uri(helloUrl)
	    .exchange()
	    .expectStatus()
	    .isOk();
	    
	}
	
	@Test
	@Order(2)
	void getLessonTypes() {
		
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
	@Order(3)
	void shouldReturnLessonsInPaginatedPages() {

	    // Recupera i tipi di lezione configurati
	    List<String> lessons = this.lessons.stream().toList();

	    // Recupera Mario, il teacher a cui verranno associate le lezioni
	    TeacherEntity teacher = teacherRepository
	            .findByEmail("mario.rossi@gmail.com")
	            .orElseThrow(() -> new NotFoundException());

	    // Verifica che Mario inizialmente non abbia lezioni
	    assertThat(lessonRepository.findByTeacher(teacher, null)).isEmpty();

	    // Crea 10 lezioni associate a Mario
	    List<LessonEntity> newLessons = rangeClosed(1001, 1010)
	            .mapToObj(i -> LessonEntity
	                    .builder()
	                    .name(lessons.get((int) Math.random() * lessons.size()))
	                    .date(LocalDate.now())
	                    .startTime(LocalTime.now())
	                    .endTime(LocalTime.now())
	                    .teacher(teacher)
	                    .build()
	            )
	            .toList();

	    // Salva le 10 lezioni nel database
	    lessonRepository.saveAll(newLessons);

	    /*
	     * Richiede la prima pagina:
	     * - page = 0       → prima pagina
	     * - size = 4       → massimo 4 elementi per pagina
	     * - ASC, "id"      → ordinamento crescente per ID
	     *
	     * Con 10 lezioni avremo quindi 3 pagine:
	     * pagina 0 → 4 elementi
	     * pagina 1 → 4 elementi
	     * pagina 2 → 2 elementi
	     */
	    Pageable nextPage = PageRequest.of(0, 4, ASC, "id");

	    // Prima pagina: recupera i primi 4 elementi
	    Page<LessonEntity> page =
	            lessonRepository.findByTeacher(teacher, nextPage);

	    // Verifica contenuto e presenza della pagina successiva
	    nextPage = testNextPage(
	            nextPage,
	            "[4, 5, 6, 7]",
	            true,
	            page
	    );

	    // Seconda pagina: utilizza il Pageable restituito dalla pagina precedente
	    page = lessonRepository.findByTeacher(teacher, nextPage);

	    // Verifica i successivi 4 elementi
	    nextPage = testNextPage(
	            nextPage,
	            "[8, 9, 10, 11]",
	            true,
	            page
	    );

	    // Terza e ultima pagina
	    page = lessonRepository.findByTeacher(teacher, nextPage);

	    // Rimangono 2 elementi e non deve esistere una pagina successiva
	    nextPage = testNextPage(
	            nextPage,
	            "[12, 13]",
	            false,
	            page
	    );
	    
	}
	
	@Test
	@Order(4)
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
	            .map(teachMapper::toLessonDto)
	            .toList();
	    
	    assertThat(expectedLessons).extracting(LessonDto::name)
	    .containsExactlyInAnyOrderElementsOf(lessonsFromResponse);
	    
	}
	
	@Test
	@Order(5)
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
	@Order(6)
	void registerLesson_returnsBadRequest_WhenLessonTypeDoesNotExist() throws Exception {

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
	    .expectStatus().isEqualTo(UNPROCESSABLE_CONTENT)
	    .expectBody()
	    .jsonPath("$.message")
	    .isEqualTo("Invalid lesson name: SWIMMING");
	   
	}
	
	@Test
	@Order(7)
	void registerLesson_returnsBadRequest_WhenLessonTimeIsWrong() throws Exception {

		String lessonName = "GYM";
	    LocalDate date = LocalDate.of(2026, 8, 25);
	    LocalTime startTime = LocalTime.of(11, 30); // inizio sbagliato
	    LocalTime endTime = LocalTime.of(10, 30); // fine sbagliata
	    
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
	    .expectStatus().isEqualTo(BAD_REQUEST)
	    .expectBody()
	    .jsonPath("$.message")
	    .isEqualTo("Invalid lesson type: SWIMMING");
	   
	}
	
	private Pageable testNextPage(
	        Pageable nextPage,
	        String expectedIds,
	        boolean expectsNextPage,
	        Page<LessonEntity> page) {

	    // Verifica che gli ID siano quelli attesi
	    assertEquals(
	        expectedIds,
	        page.getContent()
	            .stream()
	            .map(p -> p.getId())
	            .collect(Collectors.toList())
	            .toString()
	    );

	    // Verifica se esiste una pagina successiva
	    assertEquals(expectsNextPage, page.hasNext());

	    // Restituisce il riferimento alla pagina successiva
	    return page.nextPageable();
	}
	
}