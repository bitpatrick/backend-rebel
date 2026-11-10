package com.rebel.teach.integration;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import com.rebel.api.core.auth.User;
import com.rebel.api.exception.NotFoundException;
import com.rebel.util.ServiceUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
@Component
public class Integration {
	
	public static final String CIRCUIT_BREAKER_NAME = "create-teacher-sync";
	
	@Value("${app.auth-service.url:http://AUTH-SERVICE}")
	private String AUTH_SERVICE_URL;
	
	private final ReactiveCircuitBreakerFactory cbFactory;
	private final WebClient.Builder webClientBuilder;
	private final ServiceUtil serviceUtil;

	public Mono<User> createUser(User user, String token, Integer delay, Integer faultPercent) {
		
		URI url = UriComponentsBuilder
                .fromUriString(AUTH_SERVICE_URL.concat("/api/sync-signup"))
                .queryParam("delay", delay)
                .queryParam("faultPercent", faultPercent)
                .build()
                .toUri();
		
		return webClientBuilder
	        .build()
	        .post()
	        .uri(url)
	        .header("Authorization", "Bearer " + token)
	        .contentType(MediaType.APPLICATION_JSON)
	        .accept(MediaType.APPLICATION_JSON)
	        .bodyValue(user)
	        .retrieve()
	        .bodyToMono(User.class)
                .onErrorMap(WebClientResponseException.NotFound.class,
                        ex -> new NotFoundException(ex.getMessage(), ex))
	        .transform(mono -> 
	        	cbFactory.create(CIRCUIT_BREAKER_NAME).run(mono, throwable -> getUserFallbackValue(throwable)));
		
    }
	
	private Mono<User> getUserFallbackValue(Throwable ex) {

        // Non trasformare un vero 404 in una risposta di fallback.
        if (ex instanceof NotFoundException) return Mono.error(ex);

        return Mono.just(new User(
                null,
                "Fallback user",
                "Fallback user",
                "Fallback user",
                "Fallback user",
                "Fallback user",
                serviceUtil.getServiceAddress()
                ));
    }
	
}
