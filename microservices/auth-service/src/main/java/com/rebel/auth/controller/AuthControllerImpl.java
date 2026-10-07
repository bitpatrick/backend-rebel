package com.rebel.auth.controller;

import static com.rebel.api.core.event.Type.CREATE;
import static org.springframework.http.HttpStatus.CREATED;

import java.time.ZonedDateTime;
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

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@RestController
@Slf4j
@RequestMapping("/api/auth")
public class AuthControllerImpl implements AuthController {

	@Value("${roles}")
    private Set<String> roles;
	
	private final UserRepository userRepository;
	private final UserMapper userMapper;
	private final PasswordEncoder passwordEncoder;
	private final StreamBridge streamBridge;
	private final JwtService jwtService;
	private final SequenceGeneratorService sequenceGenerator;
	
	@Override
	public Mono<String> hello() {
	    log.info("{} Hello endpoint called", getClass().getSimpleName());
	    return Mono.just("Hello from " + getClass().getSimpleName());
	}

	@Override
	public Mono<ResponseEntity<Integer>> register(@Valid User user)
	        throws InvalidInputException {
	
	    log.info("Registering user with email: {}", user.email());
	
	    return userRepository.existsByEmail(user.email())
	    		/*
	    		 * riceve il Boolean emesso dal Mono
	    		 * e restituisce un'altra operazione Mono
	    		 * quindi concatena un'altra operazione reactive
	    		 */
	            .flatMap(userAlreadyExists -> {

	                if (userAlreadyExists) {
	                    return Mono.error(
	                            new InvalidInputException(
	                                    String.format(
	                                            "User with email %s already exists",
	                                            user.email())));
	                }

	                if (!roles.contains(user.role())) {
	                    return Mono.error(
	                            new NotFoundException("Some role is NOT found"));
	                }

	                return sequenceGenerator.generateSequence(UserEntity.SEQUENCE_NAME);
	                
	            }).flatMap(id -> {
	            	
	            	String passwordEncoded =
	                        passwordEncoder.encode(user.password());

	                UserEntity entity = userMapper.toEntity(user);
	                entity.setId(id);
	                entity.setPassword(passwordEncoded);

	                // save() restituisce Mono<UserEntity>
	                return userRepository.save(entity);
	            	
	            })
	            .map(userMapper::toDomain) // trasforma UserEntity in User in modo sincrono
	            
	            // riceve User e restituisce un altro Mono
	            // quindi concatena un'altra operazione reactive
	            .flatMap(savedUser -> {

	                log.info("User registered successfully - id: {}, email: {}",
	                        savedUser.id(),
	                        savedUser.email());

	                if (savedUser.role().equals("TEACHER")) {
	                	// invio evento a alla destinazione
	                    sendMessage(
	                            "teacher-created-event-out-0",
	                            new Event<>(
	                                    CREATE,
	                                    savedUser.id(),
	                                    savedUser,
	                                    ZonedDateTime.now()));

	                    return Mono.just(
	                            ResponseEntity.accepted().body(savedUser.id()));
	                }

	                return Mono.just(
	                        ResponseEntity.status(CREATED).body(savedUser.id()));
	            });
	}
	
	
	@Override
	public Mono<LoginResponse> authenticate(@Valid LoginUser loginUser)
	        throws NotFoundException, InvalidInputException {

	    log.info("Login attempt - email: {}", loginUser.email());

	    return userRepository.findByEmail(loginUser.email())
	            .log("AUTH - findByEmail")
	            .switchIfEmpty(
	                    Mono.error(new NotFoundException(String.format("User NOT found with email: %s", loginUser.email())))
	            )
	            .flatMap(user -> {

	                log.info("User found - email: {}", user.getEmail());

	                if (!passwordEncoder.matches(
	                        loginUser.password(),
	                        user.getPassword())) {

	                    log.warn("Invalid password - email: {}", loginUser.email());

	                    return Mono.error(
	                            new InvalidInputException("Invalid Password"));
	                }

	                log.info("Password validated - email: {}", loginUser.email());

	                return Mono.just(user);
	            })
	            .log("AUTH - password validation")
	            .map(user -> {
	                log.info("Generating JWT - email: {}", user.getEmail());

	                return new LoginResponse(
	                        jwtService.generateToken(user),
	                        jwtService.getExpirationTime());
	            })
	            .log("AUTH - JWT generated");
	}

	@Override
	public Mono<String> hashPassword(String password) {

	    log.info("Hashing password");

	    return Mono.just(passwordEncoder.encode(password))
	            .log("AUTH - hashPassword");
	}
	
	@Override
	public Mono<Void> deleteUser(@NotBlank @Email String email)
	        throws NotFoundException {

	    log.info("Deleting user - email: {}", email);

	    return userRepository.findByEmail(email)
	            .log("AUTH - findByEmail")

	            .switchIfEmpty(
	                    Mono.error(new NotFoundException())
	            )

	            .flatMap(user -> {
	                log.info("User found - id: {}, email: {}",
	                        user.getId(), user.getEmail());

	                return userRepository.delete(user);
	            })
	            .log("AUTH - deleteUser");
	}

	@Override
	public Mono<Page<User>> getUsers(Pageable pageable) {

	    log.info("Getting users - page: {}, size: {}",
	            pageable.getPageNumber(),
	            pageable.getPageSize());

	    return userRepository
	            .findAllBy(pageable)
	            .log("AUTH - findAllBy")

	            .map(userMapper::toDomain)
	            .log("AUTH - toDomain")

	            .collectList()
	            .log("AUTH - collectList")

	            .zipWith(userRepository.count())
	            .log("AUTH - zipWith count")

	            .map(tuple ->
	                    new PageImpl<>(
	                            tuple.getT1(),
	                            pageable,
	                            tuple.getT2()))
	            
	            ;
	}
	
	private void sendMessage(
            String bindingName,
            Event<Integer, User> event) {

        log.debug("Sending event - binding: {}, key: {}, type: {}",
                bindingName,
                event.key(),
                event.type());

        Message<Event<Integer, User>> message =
                MessageBuilder
                        .withPayload(event)
                        .setHeader("partitionKey", event.key())
                        .build();

        boolean sent = streamBridge.send(
                bindingName,
                message);

        if (sent) {
            log.info(
                    "Message sent successfully - binding: {}, key: {}",
                    bindingName,
                    event.key());
        } else {
            log.error(
                    "Failed to send message - binding: {}, key: {}",
                    bindingName,
                    event.key());
        }
    }

}
