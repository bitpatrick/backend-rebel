package com.rebel.auth.config;

import static org.springframework.security.web.server.header.XFrameOptionsServerHttpHeadersWriter.Mode.SAMEORIGIN;

import javax.crypto.SecretKey;

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

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {
	
	@Value("${security.jwt.secret-key}")
    private String secretKey;

    @Value("${security.jwt.expiration-time}")
    private int jwtExpiration;
	
	@Bean
    SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {
		
		
        return http
        		.authorizeExchange(exchanges -> exchanges

        		        // ============================================================
        		        // ENDPOINT PUBBLICI
        		        // ============================================================

        		        .pathMatchers("/api/auth/hello").permitAll()
        		        .pathMatchers("/api/auth/login").permitAll()
        		        .pathMatchers("/api/auth/hash").permitAll()
        		        .pathMatchers("/api/auth/h2-console/**").permitAll()
        		        .pathMatchers("/actuator/health").permitAll()

        		        // ============================================================
        		        // ENDPOINT PRIVATI
        		        // ============================================================

        		        .pathMatchers("/api/auth/signup").hasRole("ADMIN")

        		        // ============================================================
        		        // DEFAULT
        		        // ============================================================

        		        .anyExchange().authenticated()
        		)
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
	    grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");
	    
		JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
	    jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
	    
	    return new ReactiveJwtAuthenticationConverterAdapter(jwtAuthenticationConverter);
	}
	
}
