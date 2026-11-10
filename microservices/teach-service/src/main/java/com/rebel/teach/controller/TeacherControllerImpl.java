package com.rebel.teach.controller;

//import io.micrometer.tracing.Tracer;
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
import com.rebel.teach.mapper.TeachMapper;
import com.rebel.teach.repository.LessonRepository;
import com.rebel.teach.repository.TeacherRepository;
import com.rebel.util.ServiceUtil;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

@Validated
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class TeacherControllerImpl implements TeacherController {

	private final Scheduler jdbcScheduler;
	private final LessonRepository lessonRepository;
	private final TeacherRepository teacherRepository;
	private final TeachMapper mapper;
	private final ServiceUtil serviceUtil;
//	private final Tracer tracer;

	@Value("${lessons}")
	private Set<String> lessons;

	@Override
	public Mono<String> hello() {
		
//		log.info("Current traceId: {}", tracer.currentSpan().context().traceId());
		
	    log.info("Hello endpoint called on {}", getClass().getSimpleName());

	    return Mono.just(String.format(
	        "Hello from %s instance: %s",
	        getClass().getSimpleName(),
	        serviceUtil.getServiceAddress()
	    ));
	}

	@Override
	public Mono<Teacher> me(Jwt jwt) throws NotFoundException {

	    return Mono
	    		.justOrEmpty(jwt.getClaimAsString("sub"))
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
	            .map(mapper::toTeacherDto)
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
	    				.map(page -> page.map(mapper::toLessonDto))
	    				
	    		)
	    		.log(log.getName(), FINE)
	    		.subscribeOn(jdbcScheduler);

	}

	@Override
	public Mono<Integer> submitLessonDateTime(
	        Jwt jwt,
	        LessonDateTimeRequest request
	) throws InvalidInputException, NotFoundException {

	    return Mono.just(request)
	            .doOnSubscribe(subscription ->
	                    log.info(
	                            "[submitLessonDateTime] Sottoscrizione | thread={}",
	                            Thread.currentThread().getName()
	                    )
	            )
	            .flatMap(r -> {
	                log.info(
	                        "[submitLessonDateTime] Validazione richiesta | thread={}",
	                        Thread.currentThread().getName()
	                );

	                // L'orario di inizio deve precedere quello di fine.
	                if (!r.startTime().isBefore(r.endTime())) {
	                    return Mono.<String>error(new InvalidInputException(
	                            String.format(
	                                    "L'orario di inizio (%s) deve essere "
	                                            + "precedente all'orario di fine (%s)",
	                                    r.startTime(),
	                                    r.endTime()
	                            )
	                    ));
	                }

	                return Mono.just(r.name());
	            })
	            .filter(lessons::contains)
	            .switchIfEmpty(Mono.defer(() ->
	                    Mono.error(new InvalidInputException(
	                            "Nome lezione non valido: " + request.name()
	                    ))
	            ))
	            .map(lessonName -> jwt.getClaimAsString("sub"))
	            .flatMap(email ->
	                    // La ricerca bloccante viene eseguita sul pool JDBC.
	                    Mono.fromCallable(() -> {
	                                log.info(
	                                        "[submitLessonDateTime] Ricerca docente | thread={}",
	                                        Thread.currentThread().getName()
	                                );

	                                return teacherRepository.findByEmail(email);
	                            })
	                            .subscribeOn(jdbcScheduler)
	                            .flatMap(Mono::justOrEmpty)
	                            .switchIfEmpty(Mono.defer(() ->
	                                    Mono.error(new NotFoundException(
	                                            "Docente non trovato con email: " + email
	                                    ))
	                            ))
	            )
	            .map(teacher -> {
	                log.info(
	                        "[submitLessonDateTime] Creazione entità lezione | thread={}",
	                        Thread.currentThread().getName()
	                );

	                return mapper.createLesson(request, teacher);
	            })
	            .flatMap(lesson ->
	                    // Il salvataggio viene schedulato separatamente sul pool JDBC.
	                    Mono.fromCallable(() -> {
	                                log.info(
	                                        "[submitLessonDateTime] Salvataggio lezione | thread={}",
	                                        Thread.currentThread().getName()
	                                );

	                                return lessonRepository.save(lesson);
	                            })
	                            .subscribeOn(jdbcScheduler)
	            )
	            .map(LessonEntity::getId)
	            .doOnSuccess(id ->
	                    log.info(
	                            "[submitLessonDateTime] Lezione salvata: id={} | thread={}",
	                            id,
	                            Thread.currentThread().getName()
	                    )
	            )
	            .doOnError(error ->
	                    log.warn(
	                            "[submitLessonDateTime] Operazione fallita | thread={}",
	                            Thread.currentThread().getName(),
	                            error
	                    )
	            );
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
