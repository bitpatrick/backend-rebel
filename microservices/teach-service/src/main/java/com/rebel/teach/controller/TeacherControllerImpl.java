package com.rebel.teach.controller;

import static java.util.logging.Level.FINE;

import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rebel.api.core.teach.LessonDateTimeRequest;
import com.rebel.api.core.teach.LessonDto;
import com.rebel.api.core.teach.Teacher;
import com.rebel.api.core.teach.TeacherController;
import com.rebel.api.exception.InvalidInputException;
import com.rebel.api.exception.NotFoundException;
import com.rebel.teach.entity.LessonEntity;
import com.rebel.teach.mapper.TeachServiceMapper;
import com.rebel.teach.repository.LessonRepository;
import com.rebel.teach.repository.TeacherRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

@Validated
@RestController
@RequestMapping("/api/teach")
@RequiredArgsConstructor
@Slf4j
public class TeacherControllerImpl implements TeacherController {

	private final Scheduler jdbcScheduler;
	private final LessonRepository lessonRepository;
	private final TeacherRepository teacherRepository;
	private final TeachServiceMapper mapper;

	@Value("${lessons}")
	private Set<String> lessons;

	@Override
	public Mono<String> hello() {
	    log.info("{} Hello endpoint called", getClass().getSimpleName());
	    return Mono.just("Hello from " + getClass().getSimpleName());
	}

	@Override
	public Mono<Teacher> me(Jwt jwt) throws NotFoundException {

	    return Mono.justOrEmpty(jwt.getClaimAsString("sub"))
	            .switchIfEmpty(Mono.error(
	                    new InvalidInputException(
	                            "Teacher email is missing from JWT"
	                    )
	            ))
	            .flatMap(email ->
	                    Mono.fromCallable(() -> teacherRepository.findByEmail(email))
	                            .flatMap(Mono::justOrEmpty)
	                            .switchIfEmpty(Mono.error(
	                                    new NotFoundException(
	                                            String.format(
	                                                    "Teacher NOT Found with email: %s",
	                                                    email
	                                            )
	                                    )
	                            ))
	            )
	            .map(mapper::toDto)
	            .log(log.getName(), FINE)
	            .subscribeOn(jdbcScheduler);
	    
	}

	@Override
	public Mono<Page<LessonDto>> myLessons(Jwt jwt, Pageable pageable) {

	    log.info("Received request to retrieve teacher lessons");

	    return Mono
	    		.justOrEmpty(jwt.getClaimAsString("sub"))
	    		.switchIfEmpty(Mono.error(new InvalidInputException("Teacher email is missing from JWT")))
	    		.flatMap(email -> 
	    			
	    			Mono
	    				.fromCallable(() -> lessonRepository.findByTeacherEmail(email, pageable))
	    				.map(page -> page.map(mapper::toDto))
	    				
	    		)
	    		.log(log.getName(), FINE)
	    		.subscribeOn(jdbcScheduler);

	}

	@Override
	public Mono<Integer> submitLessonDateTime(Jwt jwt, LessonDateTimeRequest request)
			throws InvalidInputException, NotFoundException {

		return Mono.just(request.name())
				.filter(lessons::contains)
				.switchIfEmpty(Mono.error(new InvalidInputException("Invalid lesson type: " + request.name())))
				.map(lesson -> jwt.getClaimAsString("sub"))
				.flatMap(
						email -> 
							Mono
								.fromCallable(() -> teacherRepository.findByEmail(email))
								.flatMap(Mono::justOrEmpty)
								.switchIfEmpty(Mono.error(new NotFoundException(String.format("Teacher NOT Found with email: %s", email))))
							
						
						)
				.map(teacher -> mapper.createLesson(request, teacher))
				.map(lessonRepository::save)
				.log(log.getName(), FINE)
				.map(LessonEntity::getId)
				.subscribeOn(jdbcScheduler)
			;

	}

	@Override
	public Mono<Void> updateLesson(@Positive(message = "Lesson ID must be a positive integer") Integer id,
			@Valid LessonDateTimeRequest request) throws NotFoundException, InvalidInputException {

		return Mono.fromCallable(() -> lessonRepository.findById(id))
				.flatMap(Mono::justOrEmpty)
				.switchIfEmpty(Mono.error(new NotFoundException(String.format("Lesson NOT Found with id: %d", id))))
				.map(lesson -> mapper.updateLessonFromRequest(request, lesson))
				.doOnNext(lessonRepository::save)
				.log(log.getName(), FINE)
				.then()
				.subscribeOn(jdbcScheduler)
				;
				
	}

	@Override
	public Mono<Void> deleteLesson(@Positive(message = "Lesson ID must be a positive integer") Integer id)
			throws NotFoundException {
		
		return Mono.fromCallable(() -> lessonRepository.findById(id))
				.flatMap(Mono::justOrEmpty)
				.switchIfEmpty(Mono.error(new NotFoundException(String.format("Lesson NOT Found with id: %d", id))))
				.log(log.getName(), FINE)
				.doOnNext(lesson -> {
				    log.info("Deleting lesson with id: {}", lesson.getId());
				    lessonRepository.delete(lesson);
				    log.info("Lesson deleted with id: {}", lesson.getId());
				})
				.then()
				.subscribeOn(jdbcScheduler);
		
	}

	@Override
	public Mono<Set<String>> getLessonTypes() {
		return Mono.just(lessons);
	}
	
}