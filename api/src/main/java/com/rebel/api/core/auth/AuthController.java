package com.rebel.api.core.auth;

import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.rebel.api.exception.InvalidInputException;
import com.rebel.api.exception.NotFoundException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import reactor.core.publisher.Mono;

public interface AuthController {
	
	@GetMapping("/hello")
	Mono<String> hello();

    @PostMapping("/login")
    @ResponseStatus(OK)
    Mono<LoginResponse> authenticate(
            @RequestBody @Valid LoginUser loginUser)
            throws NotFoundException, InvalidInputException;

    @PostMapping("/hash")
    @ResponseStatus(OK)
    Mono<String> hashPassword(
            @RequestBody String password);

    @GetMapping("/users")
    Mono<Page<User>> getUsers(Pageable pageable);
    
    @ResponseStatus(ACCEPTED)
    @DeleteMapping("/delete")
    Mono<Void> deleteUser(
            @RequestParam @NotBlank @Email String email)
            throws NotFoundException;
    
    @PostMapping("/signup")
    Mono<ResponseEntity<User>> register(
            @RequestBody @Valid User user) throws InvalidInputException;

    // Crea l'utente e restituisce il risultato senza pubblicare eventi per Teach.
    @PostMapping("/sync-signup")
    @ResponseStatus(CREATED)
    Mono<User> registerSync(
            @RequestBody @Valid User user,
            @RequestParam(value = "delay", required = false, defaultValue = "0") int delay,
            @RequestParam(value = "faultPercent", required = false, defaultValue = "0") int faultPercent
    		) throws InvalidInputException, InterruptedException;
    
}
