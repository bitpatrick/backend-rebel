package com.rebel.auth.controller;

//import io.micrometer.tracing.Tracer;
import static com.rebel.api.core.event.Type.CREATE;
import static com.rebel.api.core.event.Type.DELETE;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.CREATED;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Random;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rebel.api.core.auth.AuthController;
import com.rebel.api.core.auth.JwtService;
import com.rebel.api.core.auth.LoginResponse;
import com.rebel.api.core.auth.LoginUser;
import com.rebel.api.core.auth.User;
import com.rebel.api.core.event.Event;
import com.rebel.api.exception.InvalidInputException;
import com.rebel.api.exception.NotFoundException;
import com.rebel.auth.entity.UserEntity;
import com.rebel.auth.mapper.UserMapper;
import com.rebel.auth.repo.UserRepository;
import com.rebel.auth.service.SequenceGeneratorService;
import com.rebel.util.ServiceUtil;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

@RequiredArgsConstructor
@RestController
@Slf4j
@RequestMapping("/api")
public class AuthControllerImpl implements AuthController {

    @Value("${roles}")
    private Set<String> roles;

    private final Scheduler publishEventScheduler;
    private final UserRepository userRepository;
    private final UserMapper mapper;
    private final PasswordEncoder passwordEncoder;
    private final StreamBridge streamBridge;
    private final JwtService jwtService;
    private final SequenceGeneratorService sequenceGenerator;
    private final ServiceUtil serviceUtil;
//    private final Tracer tracer;
    private final Random randomNumberGenerator = new Random();

    @Override
    public Mono<String> hello() {
        log.info("Received request to hello endpoint on {}", getClass().getSimpleName());

//        log.info("Current traceId: {}", tracer.currentSpan().context().traceId());
        
        return Mono.just(String.format(
            "Hello from %s instance: %s",
            getClass().getSimpleName(),
            serviceUtil.getServiceAddress()
        ));
    }

    @Override
    public Mono<LoginResponse> authenticate(@Valid LoginUser loginUser)
            throws NotFoundException, InvalidInputException {

        log.info("Authentication requested");

        return userRepository.findByEmail(loginUser.email())
                .switchIfEmpty(
                        Mono.error(new NotFoundException("User NOT found with email: " + loginUser.email()))
                )
                .flatMap(user -> {

                    if (!passwordEncoder.matches(
                            loginUser.password(),
                            user.getPassword())) {

                        log.warn("Authentication failed - invalid credentials");

                        return Mono.error(
                                new InvalidInputException("Invalid password"));
                    }

                    log.debug("User credentials validated - id: {}", user.getId());

                    return Mono.just(user);
                })
                .map(user -> {

                    LoginResponse response = new LoginResponse(
                            jwtService.generateToken(user),
                            jwtService.getExpirationTime(),
                            serviceUtil.getServiceAddress()
                    );

                    log.info("Authentication successful - userId: {}", user.getId());

                    return response;
                });
    }

    @Override
    public Mono<String> hashPassword(String password) {

        log.debug("Password hashing requested");

        return Mono.fromCallable(() -> passwordEncoder.encode(password));
    }

    @Override
    public Mono<Page<User>> getUsers(Pageable pageable) {

        log.debug("Fetching users - page: {}, size: {}",
                pageable.getPageNumber(),
                pageable.getPageSize());

        return userRepository.findAllBy(pageable)
                .map(mapper::toDomain)
                .collectList()
                .zipWith(userRepository.count())
                .map(tuple -> {

                    Page<User> page = new PageImpl<>(
                            tuple.getT1(),
                            pageable,
                            tuple.getT2()
                    );

                    log.debug("Users retrieved - count: {}, total: {}",
                            page.getNumberOfElements(),
                            page.getTotalElements());

                    return page;
                });
    }

    @Override
    public Mono<Void> deleteUser(@NotBlank @Email String email)
            throws NotFoundException {

        log.info("User deletion requested");

        return userRepository.findByEmail(email)
                .switchIfEmpty(
                        Mono.error(new NotFoundException("User not found"))
                )
                .flatMap(user -> {

                    log.debug("User found - id: {}, role: {}",
                            user.getId(), user.getRole());

                    return userRepository.delete(user)
                            .thenReturn(mapper.toDomain(user));
                })
                .flatMap(deletedUser -> {

                    log.info("User deleted successfully - id: {}",
                            deletedUser.id());

                    if ("TEACHER".equals(deletedUser.role())) {

                        return Mono.fromRunnable(() -> {

                            sendMessage(
                                    "teacher-event-out-0",
                                    new Event<>(
                                            DELETE,
                                            deletedUser.id(),
                                            deletedUser,
                                            ZonedDateTime.now()
                                    )
                            );

                        }).subscribeOn(publishEventScheduler).then();
                    }

                    return Mono.empty();
                });
    }

    private void sendMessage(
            String bindingName,
            Event<Integer, User> event) {

        log.debug("Publishing event - binding: {}, key: {}, type: {}",
                bindingName,
                event.key(),
                event.type());

        Message<Event<Integer, User>> message = MessageBuilder
                .withPayload(event)
                .setHeader("partitionKey", event.key())
                .build();

        boolean sent = streamBridge.send(bindingName, message);

        if (!sent) {
            log.error("Event publication failed - binding: {}, key: {}, type: {}",
                    bindingName,
                    event.key(),
                    event.type());

            throw new IllegalStateException("Unable to publish event: " + event.type());
        }

        log.info("Event published successfully - binding: {}, key: {}, type: {}",
                bindingName,
                event.key(),
                event.type());
    }
    
    @Override
    public Mono<ResponseEntity<User>> register(@Valid User user)
            throws InvalidInputException {

        log.info("User registration requested - email: {}", user.email());

        return userRepository.existsByEmail(user.email())
                .flatMap(alreadyExists -> {

                    if (alreadyExists) {
                        log.warn("User registration rejected - email already exists");

                        return Mono.error(
                                new InvalidInputException("User already exists"));
                    }

                    if (!roles.contains(user.role())) {
                        log.warn("User registration rejected - invalid role: {}", user.role());

                        return Mono.error(
                                new NotFoundException("Role not found"));
                    }

                    return sequenceGenerator.generateSequence(UserEntity.SEQUENCE_NAME);
                })
                .zipWith(Mono.fromCallable(() -> mapper.toEntity(user)))
                .flatMap(tuple -> {

                    long id = tuple.getT1();
                    UserEntity entity = tuple.getT2();

                    entity.setId(id);
                    entity.setPassword(passwordEncoder.encode(user.password()));

                    return userRepository.save(entity);
                })
                .map(mapper::toDomain)
                .flatMap(dto -> {

                    log.info("User registered successfully - id: {}, role: {}",
                            dto.id(), dto.role());

                    if ("TEACHER".equals(dto.role())) {

                        return Mono.fromCallable(() -> {

                            sendMessage(
                                    "teacher-event-out-0",
                                    new Event<>(
                                            CREATE,
                                            dto.id(),
                                            dto,
                                            ZonedDateTime.now()
                                    )
                            );

                            return ResponseEntity.status(ACCEPTED).body(dto);

                        }).subscribeOn(publishEventScheduler);
                    }

                    return Mono.just(
                            ResponseEntity.status(CREATED).body(dto)
                    );
                });
    }
    
    @Override
    public Mono<User> registerSync(@Valid User user, int delay, int faultPercent) throws InvalidInputException, InterruptedException {

        log.info("Synchronous user registration requested - email: {}", user.email());
        
        // simulate delay
        Thread.sleep(Duration.ofSeconds(delay));

        return userRepository.existsByEmail(user.email())
                .flatMap(alreadyExists -> {
                    
                	// check if user already exists
                	if (alreadyExists) throw new InvalidInputException("User already exists");
                	
                	// check if role is valid
                    if (!roles.contains(user.role())) throw new NotFoundException("Role not found");
                    
                    // check if bad luck
                    throwErrorIfBadLuck(faultPercent);
                    
                    // generate id
                    return sequenceGenerator.generateSequence(UserEntity.SEQUENCE_NAME);
                })
                .flatMap(id -> {
                    UserEntity entity = mapper.toEntity(user);
                    entity.setId(id);
                    entity.setPassword(passwordEncoder.encode(user.password()));
                    return userRepository.save(entity);
                })
                // Teach salva direttamente il docente dopo aver ricevuto questo DTO:
                // pubblicare anche l'evento CREATE causerebbe una seconda creazione.
                .map(mapper::toDomain)
                .doOnSuccess(created -> log.info(
                        "User synchronously registered - id: {}, email: {}",
                        created.id(), created.email()))
                .doOnError(error -> log.error(
                        "Synchronous user registration failed - email: {}",
                        user.email(), error));
    }
    
    private void throwErrorIfBadLuck(int faultPercent) {
    	
    	if(faultPercent == 0) return;
    	
    	int randomThreshold = getRandomNumber(1, 100);
    	
    	if (faultPercent < randomThreshold) {
    		 log.debug("We got lucky, no error occurred, {} < {}", 
    		 faultPercent, randomThreshold);
    		 
    		 } else {
    		 log.info("Bad luck, an error occurred, {} >= {}",
    		 faultPercent, randomThreshold);
    		 
    		 throw new RuntimeException("Something went wrong...");
		 }
    	
    }
    
    private int getRandomNumber(int min, int max) {
    	if (max < min) throw new IllegalArgumentException("Max must be greater than min");
    	return randomNumberGenerator.nextInt((max - min) + 1) + min;
    }

}
