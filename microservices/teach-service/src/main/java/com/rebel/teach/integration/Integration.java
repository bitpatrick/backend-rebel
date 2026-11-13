package com.rebel.teach.integration;

import java.net.URI;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import com.rebel.api.core.auth.User;
import com.rebel.api.exception.InvalidInputException;
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

		URI url = UriComponentsBuilder.fromUriString(AUTH_SERVICE_URL.concat("/api/sync-signup"))
				.queryParam("delay", delay).queryParam("faultPercent", faultPercent).build().toUri();

		return webClientBuilder.build().post().uri(url).header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).bodyValue(user).retrieve()
				.bodyToMono(User.class)
				// Gli errori di dominio di Auth Service diventano eccezioni di dominio PRIMA del circuit breaker:
				// così ignoreExceptions li esclude dal tasso di errore e il fallback li rilancia.
				.onErrorMap(WebClientResponseException.NotFound.class,
						ex -> new NotFoundException(extractMessage(ex), ex))
				.onErrorMap(WebClientResponseException.BadRequest.class,
						ex -> new InvalidInputException(extractMessage(ex), ex))
				// la funzione lambda viene eseguita immediatamente
				.transform(mono -> cbFactory.create(CIRCUIT_BREAKER_NAME).run(mono,
						throwable -> getUserFallbackValue(throwable)));

	}

	private Mono<User> getUserFallbackValue(Throwable ex) {

		// Il fallback viene invocato per ogni errore, anche per quelli ignorati dal circuit breaker:
		// gli errori di dominio (404, 400) vanno restituiti al client, non mascherati da un utente finto.
		if (ex instanceof NotFoundException || ex instanceof InvalidInputException)
			return Mono.error(ex);

		log.warn("Auth Service unavailable, returning fallback user - cause: {}", ex.toString());

		return Mono.just(new User(null, "Fallback user", "Fallback user", "Fallback user", "Fallback user",
				"Fallback user", serviceUtil.getServiceAddress()));
	}

	/**
	 * Estrae il campo "message" dall'HttpErrorInfo restituito da Auth Service,
	 * così il client riceve il motivo reale (es. "Role not found") e non "404 Not Found from POST ...".
	 */
	private String extractMessage(WebClientResponseException ex) {
		try {
			Map<?, ?> body = ex.getResponseBodyAs(Map.class);
			if (body != null && body.get("message") instanceof String message && !message.isBlank())
				return message;
		} catch (RuntimeException decodingError) {
			log.debug("Unable to decode Auth Service error body: {}", decodingError.toString());
		}
		return ex.getMessage();
	}

}
