package com.rebel.teach.controller;

import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rebel.api.core.teach.CreateTeacherRequest;
import com.rebel.api.core.teach.LessonDto;
import com.rebel.api.core.teach.TeachAdminController;
import com.rebel.api.core.teach.Teacher;
import com.rebel.api.exception.InvalidInputException;
import com.rebel.api.exception.NotFoundException;
import com.rebel.teach.entity.TeacherEntity;
import com.rebel.teach.filter.TeacherSpecifications;
import com.rebel.teach.integration.Integration;
import com.rebel.teach.mapper.TeachMapper;
import com.rebel.teach.repository.LessonRepository;
import com.rebel.teach.repository.TeacherRepository;
import com.rebel.util.ServiceUtil;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

@Validated
@RestController
@RequestMapping("/admin/api")
@RequiredArgsConstructor
@Slf4j
public class TeachAdminControllerImpl implements TeachAdminController {

	private final Scheduler jdbcScheduler;
    private final LessonRepository lessonRepository;
    private final TeacherRepository teacherRepository;
    private final TeachMapper mapper;
    private final ServiceUtil serviceUtil;
    private final Integration integration;

    @Value("${lessons}")
    private Set<String> lessons;

    @Override
    public Mono<String> hello() {
        log.info("Received request to hello endpoint on {}", getClass().getSimpleName());

        return Mono.just(String.format(
            "Hello from %s instance: %s",
            getClass().getSimpleName(),
            serviceUtil.getServiceAddress()
        ));
    }

    @Override
    public Mono<Page<Teacher>> teachers(
            String name,
            String surname,
            Pageable pageable) {

        log.info(
                "Retrieving teachers - name: {}, surname: {}, page: {}, size: {}",
                name, surname, pageable.getPageNumber(), pageable.getPageSize()
        );

        return Mono.fromCallable(() -> internalGetTeachers(name, surname, pageable))
                .doOnNext(page -> log.info(
                        "Retrieved {} teachers (total: {})",
                        page.getNumberOfElements(),
                        page.getTotalElements()
                ))
                .doOnError(error -> log.error("Error retrieving teachers", error))
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
                .map(mapper::toTeacherDto)
                .doOnNext(teacher -> log.info("Teacher retrieved successfully - id: {}", id))
                .doOnError(error -> log.error("Error retrieving teacher with id: {}", id, error))
                .subscribeOn(jdbcScheduler);
    }

    @Override
    public Mono<Page<LessonDto>> getTeacherLessons(
            @Positive(message = "Teacher ID must be a positive integer") Integer id,
            Pageable pageable)
            throws NotFoundException, ConstraintViolationException {

        log.info(
                "Retrieving lessons for teacher id: {} - page: {}, size: {}",
                id, pageable.getPageNumber(), pageable.getPageSize()
        );

        return Mono.fromCallable(() -> teacherRepository.findById(id))
                .flatMap(Mono::justOrEmpty)
                .switchIfEmpty(Mono.error(
                        new NotFoundException(
                                String.format("Teacher NOT Found with userId: %d", id)
                        )
                ))
                .map(teacher -> lessonRepository.findByTeacher(teacher, pageable))
                .map(page -> page.map(mapper::toLessonDto))
                .doOnNext(page -> log.info(
                        "Retrieved {} lessons for teacher id: {} (total: {})",
                        page.getNumberOfElements(),
                        id,
                        page.getTotalElements()
                ))
                .doOnError(error -> log.error(
                        "Error retrieving lessons for teacher id: {}", id, error
                ))
                .subscribeOn(jdbcScheduler);
    }

    @Override
    public Mono<Void> updateTeacher(
            @Positive(message = "Teacher ID must be a positive integer") Integer id,
            @Valid Teacher request)
            throws NotFoundException, InvalidInputException {

        log.info("Updating teacher with id: {}", id);

        return Mono.empty();
    }

    @Override
    public Mono<Void> deleteTeacher(
            @Positive(message = "UserId must be a positive integer") Integer userId)
            throws NotFoundException {

        log.info("Deleting teacher with userId: {}", userId);

        return Mono.fromCallable(() -> teacherRepository.findByUserId(userId))
                .flatMap(Mono::justOrEmpty)
                .switchIfEmpty(Mono.error(
                        new NotFoundException(
                                String.format("Teacher NOT Found with userId: %d", userId)
                        )
                ))
                .doOnNext(teacher -> log.debug(
                        "Teacher found - id: {}, userId: {}",
                        teacher.getId(), userId
                ))
                .doOnNext(teacher -> teacherRepository.delete(teacher))
                .then()
                .doOnSuccess(unused -> log.info(
                        "Teacher deleted successfully - userId: {}", userId
                ))
                .doOnError(error -> log.error(
                        "Error deleting teacher with userId: {}", userId, error
                ))
                .subscribeOn(jdbcScheduler);
    }

    @Override
    public Mono<LessonDto> getLesson(
            @Positive(message = "Lesson ID must be a positive integer") Integer id)
            throws NotFoundException {

        log.info("Retrieving lesson with id: {}", id);

        return Mono.fromCallable(() -> lessonRepository.findById(id))
                .flatMap(Mono::justOrEmpty)
                .switchIfEmpty(Mono.error(
                        new NotFoundException(
                                String.format("Lesson NOT Found with ID: %d", id)
                        )
                ))
                .map(mapper::toLessonDto)
                .doOnNext(lesson -> log.info("Lesson retrieved successfully - id: {}", id))
                .doOnError(error -> log.error("Error retrieving lesson with id: {}", id, error))
                .subscribeOn(jdbcScheduler);
    }

    private Page<Teacher> internalGetTeachers(
            String name,
            String surname,
            Pageable pageable) {

        Specification<TeacherEntity> spec = Specification.unrestricted();

        if (name != null && !name.isBlank()) {
            spec = spec.and(TeacherSpecifications.nameStartWith(name));
        }

        if (surname != null && !surname.isBlank()) {
            spec = spec.and(TeacherSpecifications.surnameStartWith(surname));
        }

        return teacherRepository.findAll(spec, pageable)
                .map(mapper::toTeacherDto);
    }

    @Override
    public Mono<Integer> createTeacher(@Valid Teacher request)
            throws InvalidInputException {

        log.info("Creating teacher with email: {}", request.email());

        return Mono.fromCallable(() -> teacherRepository.existsByEmail(request.email()))
                .flatMap(alreadyExists -> {

                    if (alreadyExists) {
                        log.warn(
                                "Teacher creation rejected: email already exists [{}]",
                                request.email()
                        );

                        return Mono.error(
                                new InvalidInputException(
                                        "A teacher with this email already exists"
                                )
                        );
                    }

                    return Mono.just(request);
                })
                .map(mapper::toTeacherEntity)
                .map(teacherRepository::save)
                .map(TeacherEntity::getId)
                .doOnSuccess(id -> log.info(
                        "Teacher created successfully - id: {}, email: {}",
                        id, request.email()
                ))
                .doOnError(error -> log.error(
                        "Error creating teacher with email: {}",
                        request.email(), error
                ))
                .subscribeOn(jdbcScheduler);
    }

    @Override
	public Mono<ResponseEntity<Teacher>> createTeacherSync(
			int delay, 
			int faultPercent, 
			Jwt jwt, 
			@Valid CreateTeacherRequest request)
            throws InvalidInputException {

        log.info("Starting teacher registration workflow - email: {}", request.email());

        return Mono
        		.fromCallable(() -> teacherRepository.existsByEmail(request.email()))

                // Verifico se il teacher esiste già in Teach
                .flatMap(alreadyExists -> {

                    if (alreadyExists) {
                        log.warn(
                                "Teacher registration rejected: email already exists [{}]",
                                request.email()
                        );

                        return Mono.error(
                                new InvalidInputException(
                                        "A teacher with this email already exists"
                                )
                        );
                    }

                    log.debug("Teacher email available: {}", request.email());

                    return Mono.just(request);
                })
                
                // creo user dto
                .map(mapper::toUserDto)

                // creo l user tramite auth service
                .flatMap(user -> {

                    log.info(
                            "Registering user in Auth Service - email: {} with delay={}s and faultPercent={}%",
                            user.email(),
                            delay,
                            faultPercent
                    );
                    
                    return integration.createUser(user, jwt.getTokenValue(), delay, faultPercent);
                    
                })
                
                // L'utente senza ID identifica il fallback dell'integrazione.
                .flatMap(user -> {
                	
                    if (user.id() == null) {
                        log.warn("Returning fallback teacher - registration not completed - email: {}",
                                request.email());
                        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                                .body(mapper.toTeacherDto(user)));
                    }
                	
                    // Solo il percorso reale salva il docente e registra la creazione riuscita.
                	return Mono
                			.fromCallable(() -> mapper.toTeacherEntity(user))
                			.map(teacherRepository::save)
                			.map(mapper::toTeacherDto)
                            .doOnNext(teacher -> log.info(
                                    "Teacher registration completed successfully - email: {}",
                                    request.email()))
                            .map(teacher -> ResponseEntity.status(HttpStatus.CREATED).body(teacher))
                            // Assegna il salvataggio bloccante allo scheduler JDBC.
                			.subscribeOn(jdbcScheduler);
                })
                
                .doOnError(error -> log.error(
                        "Teacher registration workflow failed - email: {}",
                        request.email(), error
                ))

                // Esegue la query iniziale existsByEmail sullo scheduler JDBC.
                .subscribeOn(jdbcScheduler);
        
    }

}
