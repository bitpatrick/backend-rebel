package com.rebel.teach;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.rebel.api.core.auth.JwtService;
import com.rebel.api.core.teach.TeachAdminController;
import com.rebel.teach.controller.TeachAdminControllerImpl;
import com.rebel.teach.entity.LessonEntity;
import com.rebel.teach.entity.TeacherEntity;
import com.rebel.teach.mapper.TeachServiceMapper;
import com.rebel.teach.repository.LessonRepository;
import com.rebel.teach.repository.TeacherRepository;
import com.rebel.util.UrlBuilder;

import tools.jackson.databind.ObjectMapper;

@ActiveProfiles("local")
@AutoConfigureWebTestClient(timeout = "10m")
@Transactional(propagation = Propagation.NOT_SUPPORTED) // NO ROLL BACK
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import(TestChannelBinderConfiguration.class)
class TeachAdminServiceApplicationTests {
	
	@Value("${lessons}")
	Set<String> lessons;
	
	@Autowired
	JwtService jwtService;
	
	String adminJWT;
	String teacherJWT;
	String invalidJWT;
	String staffJWT;
	
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
	String helloUrl;
	String teachersUrl;
	String registerLessonUrl;
	
	@BeforeAll
	void setupJWTs() {
		
		
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

    }
	
	@BeforeAll
	void setUpDatabase() {

	    TeacherEntity teacher1 = TeacherEntity.builder()
	    		.name("Francesco")
	    		.surname("Totti")
	    		.email("francesco.totti@gmail.com")
	    		.userId(998)
	            .build();
	    
	    TeacherEntity teacher2 = TeacherEntity.builder()
	    		.name("Luca")
	    		.surname("Bianchi")
	    		.email("luca.bianchi@gmail.com")
	    		.userId(999)
	            .build();
	    
	    TeacherEntity teacher3 = TeacherEntity.builder()
	    		.name("Andrea")
	    		.surname("Ferrari")
	    		.email("andrea.ferrari@rebel.com")
	    		.userId(906)
	            .build();
		
		TeacherEntity teacher4 = TeacherEntity.builder()
	    		.name("Stefano")
	    		.surname("Fontana")
	    		.email("stefano.fontana@rebel.com")
	    		.userId(912)
	            .build();
		
		TeacherEntity teacher5 = TeacherEntity.builder()
	    		.name("Alessandro")
	    		.surname("Ferri")
	    		.email("alessandro.ferri@rebel.com")
	    		.userId(917)
	            .build();
	    
	    teacherRepository.saveAll(List.of(teacher1, teacher2, teacher3, teacher4, teacher5));
	    
	    LessonEntity lesson1 = LessonEntity.builder()
	            .name("GYM")
	            .date(LocalDate.of(2026, 8, 25))
	            .startTime(LocalTime.of(10, 30))
	            .endTime(LocalTime.of(11, 30))
	            .teacher(teacher1)
	            .build();
	    
	    LessonEntity lesson2 = LessonEntity.builder()
	            .name("DANCE")
	            .date(LocalDate.of(2026, 8, 26))
	            .startTime(LocalTime.of(12, 0))
	            .endTime(LocalTime.of(13, 0))
	            .teacher(teacher1)
	            .build();
	    
	    LessonEntity lesson3 = LessonEntity.builder()
	            .name("GYM")
	            .date(LocalDate.of(2026, 8, 27))
	            .startTime(LocalTime.of(14, 0))
	            .endTime(LocalTime.of(15, 0))
	            .teacher(teacher2)
	            .build();
	    
	    lessonRepository.saveAll(List.of(lesson1, lesson2, lesson3));
	    
	    /*
	     * TOKEN
	     */
	    Map<String, Object> claims = new HashMap<>();
	    claims.put("email", "mario.rossi@rebel.com");
	    claims.put("roles", "ADMIN");
	    adminJWT = jwtService.generateToken(claims);

	    claims.put("email", teacher1.getEmail());
	    claims.put("roles", "TEACHER");
	    teacherJWT = jwtService.generateToken(claims);
	    
	}
	
	@Test
	void hello_returnsOkWithUnauthenticatedUser() throws Exception {

	    client.get()
	    .uri(helloUrl)
	    .accept(APPLICATION_JSON)
	    .exchange()
	    .expectStatus()
	    .isOk();
	    
	}
	
	@Test
	void getTeachers_returnsPagedTeachers_whenAdmin() throws Exception {
		
		client.get()
		.uri(teachersUrl)
		.accept(APPLICATION_JSON)
		.header("Authorization", "Bearer " + adminJWT)
		.exchange()
		.expectStatus()
		.isOk();
		
	}
//	
//	@Test
//	void getTeacher_returnsTeacher_whenAdmin() throws Exception {
//
//	    // when / then
//	    client.perform(
//	            get(teachersUrl + "/{id}", 1)
//	            .accept(APPLICATION_JSON)	
//	            .header("Authorization", "Bearer " + adminJWT)
//	        )
////	    .andDo(prettyPrint())
//	        .andExpect(status().isOk())
//	        .andExpect(jsonPath("$.id", is(1)))
//	        .andExpect(jsonPath("$.name", is("Mario")))
//	        .andExpect(jsonPath("$.surname", is("Rossi")))
//	        .andExpect(jsonPath("$.email", is("mario.rossi@gmail.com")));
//
//	    // Verifica che sia stato chiamato user-service
//	}
//	
//	@Test
//	void getTeacher_returnsNotFoundWhenTeacherDoesNotExist_whenAdmin() throws Exception {
//
//	    // when / then
//	    client.perform(
//	            get(teachersUrl + "/{id}", 999)
//	            .contentType(APPLICATION_JSON)
//	            .header("Authorization", "Bearer " + adminJWT)
//	        )
//	        .andExpect(status().isNotFound());
//
//	}
//	
//	@Test
//	void getTeacherLessons_returnsBadRequestWhenTeacherIdIsNegative_whenAdmin() throws Exception {
//
//	    client.perform(
//	            get(teachersUrl + "/{id}/lessons", -1)
//	            .accept(APPLICATION_JSON)
//	            .header("Authorization", "Bearer " + adminJWT)
//	    )
////	    .andDo(print())
//	    .andExpect(status().isBadRequest());
//	}
//	
//	@Test
//	void getParamsTeachers_returnsOk_whenAdmin() throws Exception {
//		
//		MockHttpServletResponse response = client.perform(
//				get(teachersUrl)
//				.param("name", "a")
//				.param("surname", "f")
//				.accept(APPLICATION_JSON)
//				.header("Authorization", "Bearer " + adminJWT) 
//				
//				)
//				.andExpect(status().isOk())
//				.andReturn().getResponse();
//		
//		String json = response.getContentAsString();
//		
//		List<Teacher> teachers = jsonMapper.convertValue(jsonMapper.readTree(json).get("content"), new TypeReference<List<Teacher>>() {
//		});
//		
////		assertThat(teachers).hasSize(2);
////		assertThat(teachers).extracting(Teacher::id).containsExactlyInAnyOrder(3, 4);		
//	}
	
}