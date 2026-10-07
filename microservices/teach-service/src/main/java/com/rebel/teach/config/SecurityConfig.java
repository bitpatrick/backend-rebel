package com.rebel.teach.config;

import javax.crypto.SecretKey;

import static org.springframework.security.web.server.header.XFrameOptionsServerHttpHeadersWriter.Mode.SAMEORIGIN;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;

import com.rebel.api.core.auth.JwtService;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import reactor.core.publisher.Mono;


/*
 * JWT FLOW:
 *
 * Request → JwtDecoder → Converter → Authorities → Authorization
 *
 * Auth Service → genera JWT
 * Teach Service → verifica JWT e autorizza la richiesta
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Value("${security.jwt.secret-key}")
    private String secretKey;

    @Value("${security.jwt.expiration-time}")
    private int jwtExpiration;

    @Bean
    SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http) {

        return http

            .authorizeExchange(auth -> auth

        		// =========================
                // PUBLIC
                // =========================
                .pathMatchers(
                    "/api/teach/lesson/types"
                ).permitAll()

                .pathMatchers(
                    "/**/hello"
                ).permitAll()

                .pathMatchers(
                    "/api/teach/h2-console/**"
                ).permitAll()

                .pathMatchers(
                    "/actuator/health"
                ).permitAll()

                
                // =========================
                // ADMIN
                // =========================
                .pathMatchers(
                		"/api/teach/admin/**"
                ).hasRole("ADMIN")

                // =========================
                // TEACHER
                // =========================
                .pathMatchers(
                		"/api/teach/**"
                ).hasRole("TEACHER")

                // =========================
                // OTHERS
                // =========================
                .anyExchange().authenticated()
            )

            // =========================
            // JWT RESOURCE SERVER
            // =========================

            .oauth2ResourceServer((oauth2) -> oauth2
    			.jwt(
    				(jwt) -> {
    					jwt.jwtDecoder(reactiveJwtDecoder());
    					jwt.jwtAuthenticationConverter(grantedAuthoritiesExtractor());
    				}
    			)
        	)
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
            .logout(ServerHttpSecurity.LogoutSpec::disable)
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .cors(ServerHttpSecurity.CorsSpec::disable)
            .headers(headers -> headers
        	    .frameOptions(frameOptions -> frameOptions
        	        .mode(SAMEORIGIN)
        	    )
        	)
            .build();

    }
    
    private ReactiveJwtDecoder reactiveJwtDecoder() {
		
		byte[] keyBytes = Decoders.BASE64.decode(secretKey);
	    SecretKey key = Keys.hmacShaKeyFor(keyBytes);

	    return NimbusReactiveJwtDecoder
	            .withSecretKey(key)
	            .build();
	}
	
	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
	
	@Bean
	JwtService jwtService() {
		return new JwtService(secretKey, jwtExpiration);
	}
	
	private Converter<Jwt, Mono<AbstractAuthenticationToken>> grantedAuthoritiesExtractor() {
	    
		JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
	    grantedAuthoritiesConverter.setAuthoritiesClaimName("roles");
	    grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_"); // TEACHER → ROLE_TEACHER
	    
		JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
	    jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
	    
	    return new ReactiveJwtAuthenticationConverterAdapter(jwtAuthenticationConverter);
	}

}