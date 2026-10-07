package com.rebel.teach.controller;

import static java.util.logging.Level.FINE;
import static java.util.logging.Level.INFO;

import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rebel.api.core.teach.LessonDto;
import com.rebel.api.core.teach.TeachAdminController;
import com.rebel.api.core.teach.Teacher;
import com.rebel.api.exception.InvalidInputException;
import com.rebel.api.exception.NotFoundException;
import com.rebel.teach.entity.TeacherEntity;
import com.rebel.teach.filter.TeacherSpecifications;
import com.rebel.teach.mapper.TeachServiceMapper;
import com.rebel.teach.repository.LessonRepository;
import com.rebel.teach.repository.TeacherRepository;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

@Validated
@RestController
@RequestMapping("/api/teach/admin")
@RequiredArgsConstructor
@Slf4j
public class TeachAdminControllerImpl implements TeachAdminController {

	private final Scheduler jdbcScheduler;
	private final LessonRepository lessonRepository;
	private final TeacherRepository teacherRepository;
	private final TeachServiceMapper mapper;

	@Value("${lessons}")
	private Set<String> lessons;

	@Override
	public Mono<String> hello() {
	    log.info("{} Hello endpoint called", getClass().getSimpleName());
	    return Mono.just("Hello from " + getClass().getCanonicalName());
	}
	
	@Override
	public Mono<Page<Teacher>> teachers(
	        String name,
	        String surname,
	        Pageable pageable) {

	    log.info(
	            "Retrieving teachers - name: {}, surname: {}, page: {}, size: {}",
	            name,
	            surname,
	            pageable.getPageNumber(),
	            pageable.getPageSize()
	    );

	    return Mono.fromCallable(() -> internalGetTeachers(name, surname, pageable))
	    		.log(log.getName(), INFO)
	    		.subscribeOn(jdbcScheduler);
	    
	}

	@Override
	public Mono<Teacher> getTeacher(
	        @Positive(message = "Teacher ID must be a positive integer") Integer id)
	        throws NotFoundException {

	    log.info("Retrieving teacher with id: {}", id);

	    return Mono.fromCallable(() -> teacherRepository.findById(id))
	            .flatMap(Mono::justOrEmpty)
	            .switchIfEmpty(Mono.error(
	                    new NotFoundException(
	                            String.format("Teacher NOT Found with id: %d", id)
	                    )
	            ))
	            .map(mapper::toDto)
	            .log(log.getName(), FINE)
	            .subscribeOn(jdbcScheduler);
	}

	@Override
	public Mono<Page<LessonDto>> getTeacherLessons(@Positive(message = "Teacher ID must be a positive integer") Integer id,
			Pageable pageable) throws NotFoundException, ConstraintViolationException {

		log.info("Retrieving lessons for teacher id: {} - page: {}, size: {}", id, pageable.getPageNumber(),
				pageable.getPageSize());

		return Mono.fromCallable(() -> teacherRepository.findById(id))
				.flatMap(Mono::justOrEmpty)
				.switchIfEmpty(Mono.error(new NotFoundException(String.format("Teacher NOT Found with userId: %d", id))))
				.map(teacher -> lessonRepository.findByTeacher(teacher, pageable))
				.map(page -> page.map(mapper::toDto))
				.log(log.getName(), FINE)
				.subscribeOn(jdbcScheduler);

	}

	@Override
	public Mono<Integer> createTeacher(@Valid Teacher request) throws InvalidInputException {

		log.info("Creating new teacher with email: {}", request.email());

		return Mono.fromCallable(() -> teacherRepository.existsByEmail(request.email()))
				.flatMap(alreadyExists -> {
					
					if (alreadyExists) {
						log.warn("Teacher creation failed: email already exists [{}]", request.email());
						return Mono.error(new InvalidInputException("A teacher with this email already exists"));
					}
					
					return Mono.just(request);
				})
				.map(mapper::toEntity)
				.map(teacherRepository::save)
				.map(TeacherEntity::getId)
				.log(log.getName(), FINE)
				.subscribeOn(jdbcScheduler);
		
	}

	@Override
	public Mono<Void> updateTeacher(@Positive(message = "Teacher ID must be a positive integer") Integer id,
			@Valid Teacher request) throws NotFoundException, InvalidInputException {

		return Mono.empty();
	}

	@Override
	public Mono<Void> deleteTeacher(
	        @Positive(message = "UserId must be a positive integer") Integer userId)
	        throws NotFoundException {

	    log.info("Retrieving teacher with userId: {}", userId);

	    return Mono.fromCallable(() -> teacherRepository.findByUserId(userId))
	            .flatMap(Mono::justOrEmpty)
	            .switchIfEmpty(Mono.error(
	                    new NotFoundException(
	                            String.format("Teacher NOT Found with userId: %d", userId)
	                    )
	            ))
	            .log(log.getName(), FINE)
	            .doOnNext(t -> {
	                log.info("Deleting teacher with id: {}", t.getId());
	                teacherRepository.delete(t);
	                log.info("Teacher deleted successfully with id: {}", t.getId());
	            })
	            .then()
	            .subscribeOn(jdbcScheduler);
	    		
	}

	@Override
	public Mono<LessonDto> getLesson(@Positive(message = "Lesson ID must be a positive integer") Integer id)
			throws NotFoundException {

		log.info("Lesson found with id: {}", id);

		return Mono.fromCallable(() -> lessonRepository.findById(id))
				.flatMap(Mono::justOrEmpty)
				.switchIfEmpty(Mono.error(new NotFoundException(String.format("Lesson NOT Found with ID: %d", id))))
				.map(mapper::toDto)
				.subscribeOn(jdbcScheduler);
		
	}
	
	private Page<Teacher> internalGetTeachers(String name, String surname, Pageable pageable) {
		
		Specification<TeacherEntity> spec = Specification.unrestricted();

		if (name != null && !name.isBlank()) {
		    spec = spec.and(
		            TeacherSpecifications.nameStartWith(name)
		    );
		}

		if (surname != null && !surname.isBlank()) {
		    spec = spec.and(
		            TeacherSpecifications.surnameStartWith(surname)
		    );
		}
		
		return teacherRepository
	            .findAll(spec, pageable)
	            .map(mapper::toDto);
		
	}
	
}